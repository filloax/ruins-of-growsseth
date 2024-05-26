package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.codec.CodecUtils
import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.nbt.loadField
import com.filloax.fxlib.api.nbt.saveField
import com.mojang.serialization.Codec
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.GrowssethTags.RESEARCHER_MESS_TRIGGER
import com.ruslan.growsseth.GrowssethTags.TENT_MATERIALS_WHITELIST
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.dialogues.*
import com.ruslan.growsseth.dialogues.DialogueEvent.Companion.event
import com.ruslan.growsseth.networking.AmbientSoundsPacket
import com.ruslan.growsseth.networking.StopMusicPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.util.*

class ResearcherDialoguesComponent(
    val researcher: Researcher, random: RandomSource,
    private val combat: ResearcherCombatComponent,
) : BasicDialoguesComponent(researcher, random) {
    companion object {
        // Stuff to persist in NBT (using DataFixerUpper because shorter to write even if complicated af to read)
        val CODEC_PLAYERS_IN_CELLAR: Codec<MutableSet<UUID>> = CodecUtils.setOf(UUIDUtil.STRING_CODEC)

        val EV_MAKE_MESS     = event("makeMess")
        val EV_FIX_MESS      = event("fixMess")
        val EV_REFUSE_TRADE  = event("refuseTrade")
        val EV_BREAK_TENT    = event("breakTent")
        val EV_BORROW_DONKEY = event("borrowDonkey")
        val EV_RETURN_DONKEY = event("returnDonkey")
        val EV_CELLAR        = event("exploreCellar", ignoreNoDialogueWarning = true, count = false)
        val EV_CELLAR_EXIT   = event("exitCellar", ignoreNoDialogueWarning = true, count = false)
        val PLAYER_CHEATS    = event("playerCheats")
        val KILL_PLAYER      = event("killPlayer")
        val PLAYER_ARRIVE_LAST_KILLED = event("playerArriveAfterKilled")
        val HIT_BY_PLAYER_IMMORTAL    = event("hitByPlayerImmortal")

        // "true" or unset
        const val DDATA_MADE_MESS = "madeMess"
        // "angry", "none", or unset (default)
        const val DDATA_SOUND = "sound"
        const val DDATA_SINGLE_ONLY = "singleOnly"

        val BREAK_BLOCK_BLACKLIST = mutableSetOf<Block>(
            Blocks.WHITE_CARPET,
        )
    }

    // NBT data
    private var playersInCellar: MutableSet<UUID> = mutableSetOf()
    // Unlike other dialogue-related variables,
    // share between all players as it is related to the physical block state
    // Before we tracked player ids but that meant another player couldn't fix the issue
    private var playersMadeMess: Boolean = false

    init {
        secondsForAttackRepeat = combat.timeToCalmDown / 20
    }

    override fun triggerDialogue(
        player: ServerPlayer,
        vararg dialogueEvents: DialogueEvent,
        eventParam: String?,
        ignoreEventConditions: Boolean,
    ) : Boolean {
        if (researcher.isAggressive)
            for (dialogueEvent in dialogueEvents) {
                val dialoguesForWhenAggressive =
                    listOf(BasicDialogueEvents.DEATH, BasicDialogueEvents.LOW_HEALTH, PLAYER_CHEATS, KILL_PLAYER, BasicDialogueEvents.HIT_BY_PLAYER)
                if (dialoguesForWhenAggressive.all{ it != dialogueEvent } ||
                    (dialogueEvent == BasicDialogueEvents.HIT_BY_PLAYER && combat.wantsToKillPlayer(player)))
                    return false
            }
        return super.triggerDialogue(player, *dialogueEvents, eventParam=eventParam, ignoreEventConditions=ignoreEventConditions)
    }

    override fun changeNearPlayers(nearPlayers: MutableSet<ServerPlayer>, farPlayers: MutableSet<ServerPlayer>) {
        val structureManager = (entity.level() as ServerLevel).structureManager()

        farPlayers.removeIf {
            val struct = structureManager.getStructureWithPieceAt(it.blockPosition(), GrowssethTags.StructTags.RESEARCHER_TENT)
            if (struct.isValid) {
                struct.boundingBox.isInside(it.blockPosition())
            } else {
                false
            }
        }
    }

    private fun isInCellar(player: ServerPlayer): Boolean {
        val tent = researcher.tent
        val cellarTrapdoorPos = tent?.cellarTrapdoorPos
        if (cellarTrapdoorPos != null) {
            val areaCheck = tent.cellarBoundingBox != null
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
        val players = nearPlayers + inbetweenPlayers
        for (player in players) {
            // double check if in tent, just in case
            if (isInCellar(player)) {
                triggerDialogue(player, EV_CELLAR)
            } else if (researcher.hasLineOfSight(player)) {
                triggerDialogue(player, EV_CELLAR_EXIT)
            }
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
        val hadMess = playersMadeMess
        when(event) {
            EV_MAKE_MESS   -> playersMadeMess = true
            EV_FIX_MESS    -> playersMadeMess = false
            EV_CELLAR      -> {
                playersInCellar.add(player.uuid)
                if (triggerSuccess) {
                    ServerPlayNetworking.send(player, StopMusicPacket())
                    ServerPlayNetworking.send(player, AmbientSoundsPacket())
                }
            }
            EV_CELLAR_EXIT -> playersInCellar.remove(player.uuid)
        }
        if (hadMess && !playersMadeMess) {
            researcher.angryForMess = false
        } else if (!hadMess && playersMadeMess) {
            researcher.angryForMess = true
            researcher.setUnhappy()
        }
    }

    override fun onPlayerArrive(player: ServerPlayer) {
        if (combat.lastKilledPlayers.contains(player)) {
            triggerDialogue(player, PLAYER_ARRIVE_LAST_KILLED)
            combat.lastKilledPlayers.remove(player)
        }
        else
            super.onPlayerArrive(player)
    }

    override fun canTriggeredEventRun(player: ServerPlayer, dialogueEvent: DialogueEvent): Boolean {
        return super.canTriggeredEventRun(player, dialogueEvent) && when(dialogueEvent) {
            EV_MAKE_MESS   -> !playersMadeMess
            EV_FIX_MESS    ->  playersMadeMess
            EV_CELLAR      -> !playersInCellar.contains(player.uuid)
            EV_CELLAR_EXIT ->  playersInCellar.contains(player.uuid)
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
    }

    override fun addExtraNbtData(dialogueData: CompoundTag) {
        super.addExtraNbtData(dialogueData)

        // Remove shared data, save separately
        dialogueData.remove(DataFields.SAVED_PLAYERS_DATA)

        dialogueData.saveField("MessAngerActive", Codec.BOOL, this::playersMadeMess)
        dialogueData.saveField("PlayersInCellar", CODEC_PLAYERS_IN_CELLAR, this::playersInCellar)
    }

    override fun readExtraNbtData(dialogueData: CompoundTag) {
        // prevent modifying shared data
        val savedPlayersDataPre = savedPlayersData.mapValues { it.value.copy() }
        super.readExtraNbtData(dialogueData)
        savedPlayersData.clear()
        savedPlayersData.putAll(savedPlayersDataPre)

        dialogueData.loadField("MessAngerActive", Codec.BOOL) { playersMadeMess = it }
        dialogueData.loadField("PlayersInCellar", CODEC_PLAYERS_IN_CELLAR) { playersInCellar = it.toMutableSet() }
    }

    // Save nbt data to be shared between researcher entities in single researcher mode
    fun saveSharedData(data: CompoundTag) {
        data.put("SharedDialogueData", CompoundTag().apply {
            saveField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC), ::savedPlayersData)
        })
    }

    // Save nbt data to be shared between researcher entities in single researcher mode.
    // Preferably run AFTER the base readNbtData of dialoguecomponent as it wipes saved players data,
    // we try to handle this in readExtraNbtData but better safe than sorry
    fun readSharedData(data: CompoundTag) {
        savedPlayersData.clear()
        data.getCompound("SharedDialogueData").apply {
            loadField(DataFields.SAVED_PLAYERS_DATA, mutableMapCodec(UUIDUtil.STRING_CODEC, PLAYER_DATA_CODEC)) { savedPlayersData.putAll(it) }
        }
    }

    override fun sendDialogueToPlayer(player: ServerPlayer, line: DialogueLine) {
        super.sendDialogueToPlayer(player, line)
        val soundData = line.dialogue.data[DDATA_SOUND]
        if (soundData != "none") {
            when (soundData) {
                "angry" -> researcher.playSound(SoundEvents.WANDERING_TRADER_NO)
                else -> researcher.playAmbientSound()
            }
            researcher.resetAmbientSoundTime()
        }
    }

    object Callbacks {
        fun onBlockBreak(level: Level, player: Player, pos: BlockPos, state: BlockState, entity: BlockEntity?) {
            if (level !is ServerLevel) return
            if (state.`is`(RESEARCHER_MESS_TRIGGER)) {
                val researchersInBounds = getResearchersNearTentAt(level, pos) ?: return
                researchersInBounds.forEach {
                    it.dialogues?.triggerDialogue(player as ServerPlayer, EV_MAKE_MESS)
                }
            } else if (state.`is`(TENT_MATERIALS_WHITELIST) && !BREAK_BLOCK_BLACKLIST.contains(state.block)) {
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
                it.dialogues?.triggerDialogue(player as ServerPlayer, EV_FIX_MESS)
            }
        }

        // Also checks if pos is in the tent
        private fun getResearchersNearTentAt(level: ServerLevel, pos: BlockPos): List<Researcher>? {
            val structureManager = level.structureManager()

            val structureStart = structureManager.getStructureWithPieceAt(pos, GrowssethTags.StructTags.RESEARCHER_TENT)
            if (structureStart.isValid) {
                val bbox = structureStart.boundingBox
                return level.getEntitiesOfClass(
                    Researcher::class.java,
                    AABB.of(bbox).expandTowards(Researcher.WALK_LIMIT_DISTANCE.toDouble(), 0.0, Researcher.WALK_LIMIT_DISTANCE.toDouble())
                )
            }
            return null
        }
    }
}