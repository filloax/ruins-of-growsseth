package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.codec.CodecUtils
import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.nbt.loadField
import com.filloax.fxlib.api.nbt.saveField
import com.filloax.fxlib.api.networking.sendPacket
import com.mojang.serialization.Codec
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.GrowssethTags.RESEARCHER_MESS_TRIGGER
import com.ruslan.growsseth.GrowssethTags.TENT_MATERIALS_WHITELIST
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.dialogues.*
import com.ruslan.growsseth.dialogues.DialogueEvent.Companion.event
import com.ruslan.growsseth.network.AmbientSoundsPacket
import com.ruslan.growsseth.network.StopMusicPacket
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.utils.notNull
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.util.*

class ResearcherDialoguesComponent(
    val researcher: Researcher, random: RandomSource,
    private val combat: ResearcherCombatComponent,
) : BasicDialoguesComponent(researcher, random) {
    companion object {
        // Stuff to persist in NBT (using DataFixerUpper because shorter to write even if complicated af to read)
        val CODEC_PLAYERSET: Codec<MutableSet<UUID>> = CodecUtils.setOf(UUIDUtil.STRING_CODEC)

        val EV_MAKE_MESS     = event("makeMess", immediate = true)
        val EV_FIX_MESS      = event("fixMess", immediate = true)
        val EV_REFUSE_TRADE  = event("refuseTrade", immediate = true)
        val EV_BREAK_TENT    = event("breakTent", immediate = true)
        val EV_RETURN_DONKEY = event("returnDonkey", immediate = true)
        val EV_CELLAR        = event("exploreCellar", count = false, immediate = true)
        val EV_CELLAR_EXIT   = event("exitCellar", count = false, immediate = true)
        val EV_BORROW_DONKEY = event("borrowDonkey", immediate = true)
        val EV_BORROW_DONKEY_HEALED         = event("borrowDonkeyHealed", immediate = true)
        val EV_PLAYER_CHEATS                = event("playerCheats", immediate = true)
        val EV_KILL_PLAYER                  = event("killPlayer", immediate = true)
        val EV_PLAYER_ARRIVE_LAST_KILLED    = event("playerArriveAfterKilled")
        val EV_HIT_BY_PLAYER_IMMORTAL       = event("hitByPlayerImmortal", immediate = true)
        val EV_ARRIVE_NEW_LOCATION          = event("playerArriveNewLocation", ignoreNoDialogueWarning = true)

        // Mod compatibility features
        val EV_COMPAT_COBBLEMON_BATTLE_START        = event("compatCobblemonBattleStart", ignoreNoDialogueWarning = true)
        val EV_COMPAT_COBBLEMON_BATTLE_LAST_POKEMON = event("compatCobblemonBattleLastPkmn", ignoreNoDialogueWarning = true)
        val EV_COMPAT_COBBLEMON_BATTLE_END_WIN      = event("compatCobblemonBattleEndWin", ignoreNoDialogueWarning = true)
        val EV_COMPAT_COBBLEMON_BATTLE_END_LOSE     = event("compatCobblemonBattleEndLose", ignoreNoDialogueWarning = true)
        val EV_COMPAT_COBBLEMON_ERROR               = event("compatCobblemonError", ignoreNoDialogueWarning = true)

        val AGGRESSIVE_DIALOGUE_EVENTS_ALLOWED = listOf(
            BasicDialogueEvents.DEATH,
            BasicDialogueEvents.LOW_HEALTH,
            EV_PLAYER_CHEATS,
            EV_KILL_PLAYER,
            BasicDialogueEvents.HIT_BY_PLAYER,
        )

        // "true" or unset
        const val DDATA_MADE_MESS = "madeMess"
        // "angry", "none", or unset (default)
        const val DDATA_SOUND = "sound"
        const val DDATA_SINGLE_ONLY = "singleOnly"

        // mod compat
        const val DDATA_COBBLEMON_ERROR = "errorId"

        val BREAK_BLOCK_BLACKLIST = mutableSetOf<Block>(
            Blocks.WHITE_CARPET,
        )
    }

    // NBT data
    private var playersInCellar: MutableSet<UUID> = mutableSetOf()
    // Used for different trade refusal dialogue when another player made a mess
    private val playersWhoMadeMess: MutableSet<UUID> = mutableSetOf()
    // Variables for keeping count of items inside tent for madeMess trigger
    private var cartographyTablesInTent: Int = 1
    private var lecternsInTent: Int = 1
    // If the player met this specific instance of the researcher (not other entities)
    // meaningful only in single researcher mode
    private val playersMetThisEntity: MutableSet<UUID> = mutableSetOf()

    init {
        secondsForAttackDiagRepeat = combat.ticksToCalmDown / 20
        maxCloseHitsForDialogues = combat.maxHitCounter
    }

    override fun triggerDialogue(
        player: ServerPlayer,
        vararg dialogueEvents: DialogueEvent,
        eventParam: String?,
        ignoreEventConditions: Boolean,
    ) {
        if (researcher.isAggressive)
            for (dialogueEvent in dialogueEvents) {
                if (
                    AGGRESSIVE_DIALOGUE_EVENTS_ALLOWED.all{ it != dialogueEvent }
                    || (dialogueEvent == BasicDialogueEvents.HIT_BY_PLAYER && combat.wantsToKillPlayer(player))
                )
                    return
            }
        super.triggerDialogue(player, *dialogueEvents, eventParam=eventParam, ignoreEventConditions=ignoreEventConditions)
    }

    override fun changeNearPlayers(nearPlayers: MutableSet<ServerPlayer>, farPlayers: MutableSet<ServerPlayer>) {
        val structureManager = (entity.level() as ServerLevel).structureManager()

        farPlayers.removeIf {
            val struct = structureManager.getStructureWithPieceAt(it.blockPosition(), GrowssethTags.StructTags.RESEARCHER_TENT)
            if (struct.isValid) {
                struct.boundingBox.inflatedBy(5).isInside(it.blockPosition())
            } else {
                false
            }
        }
    }

    private fun isInCellar(player: ServerPlayer): Boolean {
        // double check if in tent, just in case
        val tent = researcher.tent
        val cellarTrapdoorPos = tent?.cellarTrapdoorPos
        if (notNull(cellarTrapdoorPos)) {
            val areaCheck = notNull(tent.cellarBoundingBox)
            // double check if in tent, just in case
            return (tent.boundingBox.isInside(player.blockPosition())
                && (
                    (areaCheck && tent.cellarBoundingBox?.isInside(player.blockPosition()) == true)
                    || (!areaCheck && player.blockPosition().y < cellarTrapdoorPos.y - 5)
                ))
        }
        return false
    }

    override fun afterPlayersCheck(
        nearPlayers: Set<ServerPlayer>,
        inbetweenPlayers: Set<ServerPlayer>,
        farPlayers: Set<ServerPlayer>
    ) {
        val cellarCheck = !researcher.healed        // we check the cellar only if the researcher has not been healed yet
        if (cellarCheck) {
            val players = nearPlayers + inbetweenPlayers
            for (player in players) {
                if (!player.isSpectator) {
                    val inCellar = isInCellar(player)
                    if (inCellar && !playersInCellar.contains(player.uuid)) {
                        playersInCellar.add(player.uuid)
                        triggerDialogue(player, EV_CELLAR)
                    } else if (!inCellar && playersInCellar.contains(player.uuid) && researcher.hasLineOfSight(player)) {
                        playersInCellar.remove(player.uuid)
                        triggerDialogue(player, EV_CELLAR_EXIT)
                    }
                }
            }
        } else {
            playersInCellar.clear()     // it's easier to do it here
        }
    }

    override fun incrementEventCount(event: DialogueEvent, player: ServerPlayer) {
        if (event == BasicDialogueEvents.HIT_BY_PLAYER) {
            val pdata = playerDataOrCreate(player)
            pdata.eventLastTriggerTime[event] = entity.level().gameTime
            pdata.eventTriggerCount[event] = pdata.eventTriggerCount.getOrDefault(event, 0) + 1
            pdata.eventCloseTriggerCount[event] = combat.hitCounter.getOrDefault(player, 0).toInt() + 1       // + 1 because can't put 0 in afterCloseRepeatsMax
            RuinsOfGrowsseth.logDev(org.apache.logging.log4j.Level.INFO, "Triggered $event ${pdata.eventTriggerCount[event]} times (close ${pdata.eventCloseTriggerCount[event] ?: 0})")
        }
        else
            super.incrementEventCount(event, player)
    }

    override fun onEventSelected(event: DialogueEvent, eventParam: String?, player: ServerPlayer, triggerSuccess: Boolean) {
        super.onEventSelected(event, eventParam, player, triggerSuccess)
        // Check for mess condition is already in CanTriggeredEventRun
        when(event) {
            EV_MAKE_MESS   -> {
                researcher.angryForMess = true
                researcher.setUnhappy()
            }
            EV_FIX_MESS    -> {
                researcher.angryForMess = false
            }
            EV_CELLAR      -> {
                if (triggerSuccess) {
                    player.sendPacket(StopMusicPacket())
                    player.sendPacket(AmbientSoundsPacket())
                }
            }
        }
    }

    override fun onPlayerArrive(player: ServerPlayer) {
        val justMet = !playersMetThisEntity.contains(player.uuid)
        playersMetThisEntity.add(player.uuid)

        // Stops random strolls when players approach him (by resetting his navigation path to himself)
        researcher.navigation.moveTo(researcher, Researcher.WALKING_SPEED)
        researcher.navigation.recomputePath()

        val triggeredArriveBefore = (playerData(player)?.eventTriggerCount
            ?.filter { it.key in setOf(
                BasicDialogueEvents.PLAYER_ARRIVE_SOON,
                BasicDialogueEvents.PLAYER_ARRIVE_NIGHT,
                BasicDialogueEvents.PLAYER_ARRIVE_LONG_TIME,
                BasicDialogueEvents.PLAYER_ARRIVE,
            ) }
            ?.values?.sum()
            ?: 0) > 0

        if (combat.lastKilledPlayers.contains(player)) {
            triggerDialogue(player, EV_PLAYER_ARRIVE_LAST_KILLED)
            combat.lastKilledPlayers.remove(player)
        } else if (ResearcherConfig.singleResearcher && justMet && triggeredArriveBefore) {
            triggerDialogue(player, EV_ARRIVE_NEW_LOCATION)
        } else {
            super.onPlayerArrive(player)
        }
    }

    override fun onPlayerTickNear(player: ServerPlayer) {
        if (combat.lastKilledPlayers.contains(player)) {
            triggerDialogue(player, EV_PLAYER_ARRIVE_LAST_KILLED)
            combat.lastKilledPlayers.remove(player)
        } else {
            super.onPlayerTickNear(player)
        }
    }

    override fun canTriggeredEventRun(player: ServerPlayer, dialogueEvent: DialogueEvent): Boolean {
        return super.canTriggeredEventRun(player, dialogueEvent) && when(dialogueEvent) {
            EV_MAKE_MESS   -> !playersMadeMess()
            EV_FIX_MESS    ->  playersMadeMess()
            EV_BREAK_TENT  -> !playersInCellar.contains(player.uuid)
            else -> true
        }
    }

    override fun addDialogueOptionFilters(
        filters: MutableList<(DialogueEntry) -> Boolean>,
        player: ServerPlayer,
        event: DialogueEvent,
        eventParam: String?
    ) {
        super.addDialogueOptionFilters(filters, player, event, eventParam)

        filters.add { entry -> entry.data[DDATA_SINGLE_ONLY] != "true" || ResearcherConfig.singleResearcher }

        // Check made mess dialogue - only negatively, so remove madeMess dialogue if not angry
        // but not normal dialogue if angry, as doing so would remove also higher priority dialogue.
        // To use it properly, make it have higher priority than normal dialogues, but lower than quests etc.
        if (!researcher.angryForMess) {
            filters.add { entry -> entry.data[DDATA_MADE_MESS] != "true" }
        }

        if (RuinsOfGrowsseth.modCompat.isAllCobblemonDepsLoaded && event == EV_COMPAT_COBBLEMON_ERROR) {
            filters.add { entry -> entry.data[DDATA_COBBLEMON_ERROR] == eventParam }
        }
    }

    override val saveNbtPersistData: Boolean = false

    override fun addExtraNbtData(dialogueData: CompoundTag) {
        super.addExtraNbtData(dialogueData)

        dialogueData.saveField("PlayersWhoMadeMess", CODEC_PLAYERSET, this::playersWhoMadeMess)
        dialogueData.saveField("CartographyTablesInTent", Codec.INT, this::cartographyTablesInTent)
        dialogueData.saveField("LecternsInTent", Codec.INT, this::lecternsInTent)
        dialogueData.saveField("PlayersInCellar", CODEC_PLAYERSET, this::playersInCellar)
        dialogueData.saveField("PlayersMetThisEntity", CODEC_PLAYERSET, this::playersMetThisEntity)
    }

    override fun readExtraNbtData(dialogueData: CompoundTag) {
        // prevent modifying shared data
        super.readExtraNbtData(dialogueData)

        dialogueData.loadField("PlayersWhoMadeMess", CODEC_PLAYERSET) { playersWhoMadeMess.addAll(it)}
        dialogueData.loadField("CartographyTablesInTent", Codec.INT) { cartographyTablesInTent = it }
        dialogueData.loadField("LecternsInTent", Codec.INT) { lecternsInTent = it }
        dialogueData.loadField("PlayersInCellar", CODEC_PLAYERSET) { playersInCellar = it.toMutableSet() }
        playersMetThisEntity.clear()
        dialogueData.loadField("PlayersMetThisEntity", CODEC_PLAYERSET) { playersMetThisEntity.addAll(it) }
    }

    // Save nbt data to be shared between researcher entities in single researcher mode
    fun saveSharedData(data: CompoundTag) {
        data.put("SharedDialogueData", CompoundTag().apply {
            saveField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC), ::savedPlayersData)
        })
    }

    // Save nbt data to be shared between researcher entities in single researcher mode.
    // Preferably run AFTER the base readNbtData of DialogueComponent as it wipes saved players data,
    // we try to handle this in readExtraNbtData but better safe than sorry
    fun readSharedData(data: CompoundTag) {
        savedPlayersData.clear()
        data.getCompound("SharedDialogueData").apply {
            loadField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC)) { savedPlayersData.putAll(it) }
        }
    }

    fun playersMadeMess(): Boolean {
        return playersWhoMadeMess.isNotEmpty()
    }

    fun playerMadeMess(playerUuid: UUID): Boolean {
        return playersWhoMadeMess.contains(playerUuid)
    }

    override fun sendDialogueToPlayer(player: ServerPlayer, line: DialogueLineProcessed) {
        super.sendDialogueToPlayer(player, line)
        val soundData = line.dialogue.data[DDATA_SOUND]
        if (soundData != "none") {
            when (soundData) {
                "angry" -> researcher.playSound(GrowssethSounds.RESEARCHER_NO)
                else -> researcher.playAmbientSound()
            }
            researcher.resetAmbientSoundTime()
        }
    }

    object Callbacks {
        fun onBlockBreak(level: Level, player: Player, pos: BlockPos, state: BlockState) {
            if (level !is ServerLevel) return
            if (state.`is`(RESEARCHER_MESS_TRIGGER)) {
                val researchersInBounds = getResearchersNearTentAt(level, pos) ?: return
                researchersInBounds.forEach {
                    val dialogues = it.dialogues!!
                    if (state.`is`(Blocks.CARTOGRAPHY_TABLE))
                        dialogues.cartographyTablesInTent--
                    else if (state.`is`(Blocks.LECTERN))
                        dialogues.lecternsInTent--
                    if (dialogues.cartographyTablesInTent < 1 || dialogues.lecternsInTent < 1) {
                        dialogues.triggerDialogue(player as ServerPlayer, EV_MAKE_MESS)
                        dialogues.playersWhoMadeMess.add(player.uuid)
                    }
                }
            }
            else if (state.`is`(TENT_MATERIALS_WHITELIST) && !BREAK_BLOCK_BLACKLIST.contains(state.block)) {
                val researchersInBounds = getResearchersNearTentAt(level, pos) ?: return
                researchersInBounds.forEach {
                    it.dialogues?.triggerDialogue(player as ServerPlayer, EV_BREAK_TENT)
                }
            }
        }

        fun onPlaceBlock(player: Player, level: Level, pos: BlockPos, placeContext: BlockPlaceContext, blockState: BlockState, item: BlockItem) {
            if (level !is ServerLevel) return
            if (!blockState.`is`(RESEARCHER_MESS_TRIGGER)) return
            if (player.isSpectator) return

            val researchersInBounds = getResearchersNearTentAt(level, pos) ?: return
            researchersInBounds.forEach {
                val dialogues = it.dialogues!!
                if (blockState.`is`(Blocks.CARTOGRAPHY_TABLE))
                    dialogues.cartographyTablesInTent++
                else if (blockState.`is`(Blocks.LECTERN))
                    dialogues.lecternsInTent++
                if (dialogues.cartographyTablesInTent >= 1 && dialogues.lecternsInTent >= 1) {
                    dialogues.triggerDialogue(player as ServerPlayer, EV_FIX_MESS)
                    dialogues.playersWhoMadeMess.clear()
                }
            }
        }

        // Also checks if pos is in the tent
        private fun getResearchersNearTentAt(level: ServerLevel, pos: BlockPos): List<Researcher>? {
            val structureManager = level.structureManager()
            val structureStart = structureManager.getStructureWithPieceAt(pos, GrowssethTags.StructTags.RESEARCHER_TENT)
            if (structureStart.isValid) {
                val tentBbox = structureStart.boundingBox
                val bboxInflation = Researcher.WALK_LIMIT_DISTANCE.toDouble()   // can break only by moving the researcher away with a boat
                return level.getEntitiesOfClass(
                    Researcher::class.java,
                    AABB.of(tentBbox).inflate(bboxInflation, 0.0, bboxInflation)
                )
            }
            return null
        }
    }
}