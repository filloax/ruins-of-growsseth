package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.alwaysTruePredicate
import com.filloax.fxlib.api.codec.decodeNbt
import com.filloax.fxlib.api.codec.encodeNbt
import com.filloax.fxlib.api.codec.throwableCodecErr
import com.filloax.fxlib.api.enums.SetBlockFlag
import com.filloax.fxlib.api.iterBlocks
import com.filloax.fxlib.api.loreLines
import com.filloax.fxlib.api.nbt.getCompoundOrNull
import com.filloax.fxlib.api.nbt.getOrPut
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.trades.ProgressResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTradeMode
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.quests.*
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import com.ruslan.growsseth.templates.BookTemplates
import com.ruslan.growsseth.utils.isNull
import com.ruslan.growsseth.utils.notNull
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Clearable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Class for updating the "final quest" of the researcher,
 * where he gets zombified and placed inside his tent and can be cured.
 * General progress:
 * start: When configured API quest #1 is triggered: Unlock dialogue regarding illness
 * zombie: When configured API quest #2 is triggered: Transform into zombie, teleport to basement
 * -: If killed, that's it
 * healed: If cured, unlock dialogue regarding that
 * home: Chunk reloaded: teleport to starting position, unlock departure dialogue
 * wait: Wait a day before leaving
 * ending: Chunk reloaded: vanish
 */
class ResearcherQuestComponent(researcher: Researcher) : QuestComponent<Researcher>(researcher, QUEST_NAME) {
    object Stages {
        const val START = "start"
        const val ZOMBIE = "zombie"
        const val HEALED = "healed"
        const val HEALED_WAIT = "healed_wait"
        const val HOME = "home"
        const val HOME_WAIT = "home_wait"
        const val ENDING = "ending"
    }

    override val availableStages = Stages::class.java.declaredFields
        .filter { it.type == String::class.java }
        .sortedBy { it.declaringClass.declaredFields.indexOf(it) }
        .map { it.get(null) as String }

    private val finalQuestStartName = "researcher_end_quest_start"
    private val finalQuestZombieName = "researcher_end_quest_zombie"
    private val finalQuestLeaveName = "researcher_end_quest_leave"

    // Trigger for generic "wait for player to leave or time to pass" criteria
    private val commonReloadTrigger = (
            EventTrigger<Researcher>(QuestUpdateEvent.LOAD)
            or NoPlayersInRadiusTrigger(this, chunkRadius = 8)
            or TimeOrDayTimeTrigger(this, Constants.DAY_TICKS_DURATION)
        )


    // Used to avoid repeating full tent removal with multiple tents in normal worlds
    private var alreadyRemovedTent = false

    init {
        addStage(Stages.START, StartStage())
        // can skip start
        addStage(Stages.ZOMBIE, ZombieStage(), Stages.START, INIT_STAGE_ID, priority = -10, blockSiblingStages = true)
        addStage(Stages.HEALED, HealedStage(), Stages.ZOMBIE)
        addStage(Stages.HEALED_WAIT, HealedWaitForDialogueStage(), Stages.ZOMBIE)
        addStage(Stages.HOME, HomeLastDialogueStage(), Stages.HEALED, blockNextStages = true)
        addStage(Stages.HOME_WAIT, WaitBeforeLeaveStage(), Stages.HOME, blockNextStages = true)
        addStage(Stages.ENDING, EndingStage(), Stages.HOME_WAIT)
    }

    override fun writeCustomNbt(tag: CompoundTag) {
        tag.putBoolean(TAG_ALREADY_REMOVED_TENT, alreadyRemovedTent)
    }

    override fun readCustomNbt(tag: CompoundTag) {
        alreadyRemovedTent = tag.getBoolean(TAG_ALREADY_REMOVED_TENT)
    }

    companion object {
        const val QUEST_NAME = "researcherIllness"
        const val TAG_ALREADY_REMOVED_TENT = "alreadyRemovedTent"

        // Used to change diaries and dialogues if the player explores all structures before meeting the researcher
        private var playerSkippedQuest = false

        fun getPersistentData(server: MinecraftServer): QuestData {
            if (!ResearcherConfig.singleResearcher) {
                RuinsOfGrowsseth.LOGGER.error("Tried getting researcher quest data when not in single researcher mode!")
                return QuestData()
            }
            val tag = ResearcherSavedData.getPersistent(server).data.getCompound(QUESTS_TAG_ID).getCompound(QUEST_NAME).getCompoundOrNull(NBT_TAG_PERSIST)
            return tag?.let { PERSIST_CODEC.decodeNbt(it) .getOrThrow(
                throwableCodecErr("ResearcherQuestComponent getPersistentData")
            ).first }
                ?: QuestData()
        }

        fun writePersistentData(server: MinecraftServer, data: QuestData) {
            if (!ResearcherConfig.singleResearcher) {
                RuinsOfGrowsseth.LOGGER.error("Tried writing researcher quest data when not in single researcher mode!")
            }
            val researcherData = ResearcherSavedData.getPersistent(server)
            val questsTag = researcherData.data.getOrPut(QUESTS_TAG_ID, CompoundTag()).getOrPut(QUEST_NAME, CompoundTag())
            questsTag.put(NBT_TAG_PERSIST, PERSIST_CODEC.encodeNbt(data).getOrThrow(throwableCodecErr("writePersistentData quest")))
            researcherData.setDirty()
            updateCurrentResearchers(server)
        }

        private fun updateCurrentResearchers(server: MinecraftServer) {
            server.allLevels.forEach { level ->
                level.getEntities(GrowssethEntities.RESEARCHER, alwaysTruePredicate()).forEach { researcher ->
                    researcher.readSavedData(ResearcherSavedData.getPersistent(server))
                }
            }
        }

        fun isHealed(server: MinecraftServer): Boolean {
            return getPersistentData(server).stageHistory.contains(Stages.HEALED)
        }

        fun shouldRemoveTent(server: MinecraftServer): Boolean {
            return getPersistentData(server).stageHistory.contains(Stages.ENDING)
        }

        /**
         * Note: doesn't care about stage order
         */
        fun setStage(server: MinecraftServer, stage: String) {
            assert(stage in listOf(Stages.HOME, Stages.HOME_WAIT, Stages.START, Stages.HEALED, Stages.ZOMBIE, Stages.ENDING))
                { "Stage $stage not included in stages for researcher!" }
            val data = getPersistentData(server)
            data.currentStageId = stage
            data.stageHistory.add(stage)
            data.currentStageTriggerTime = server.overworld().gameTime
            writePersistentData(server, data)
        }

        fun backOneStage(server: MinecraftServer): Boolean {
            val data = getPersistentData(server)
            data.currentStageId = data.stageHistory.removeLastOrNull() ?: return false
            writePersistentData(server, data)
            return true
        }

        fun removeTentAndResearcher(researcher: Researcher) {
            val level = researcher.level() as ServerLevel
            RuinsOfGrowsseth.LOGGER.info("Removing tent and researcher $this")
            level.server.run {
                researcher.tent?.let { tent ->
                    val giftPos = tent.boundingBox.center.above(2)
                    val tentRotation = tent.placeSettings().rotation
                    tent.remove(level, replaceUndergroundEntrance = true)
                    spawnRewardChest(level, giftPos, tentRotation)
                }
                removeResearcher(researcher)
            }
        }

        fun removeResearcher(researcher: Researcher) {
            val level = researcher.level() as ServerLevel
            if (!researcher.donkeyWasBorrowed) {
                ResearcherDonkey.removeDonkey(researcher, level)
            }
            val savedData = ResearcherSavedData.getPersistent(level.server)
            researcher.writeSavedData(savedData)
            savedData.setDirty()
            researcher.discard()
        }

        fun spawnRewardChest(level: ServerLevel, pos: BlockPos, tentRotation: Rotation) {
            val prevBlockEntity = level.getBlockEntity(pos)
            Clearable.tryClear(prevBlockEntity)

            // chest always spawns facing the campfire
            val chestState: BlockState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, tentRotation.rotate(Direction.NORTH))

            level.setBlock(pos, chestState, SetBlockFlag.or(
                SetBlockFlag.NOTIFY_CLIENTS,
                SetBlockFlag.NO_NEIGHBOR_REACTIONS,
                SetBlockFlag.NO_NEIGHBOR_REACTION_DROPS,
            ))
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity == null || blockEntity !is BaseContainerBlockEntity) {
                RuinsOfGrowsseth.LOGGER.error("No blockentity at reward chest pos $pos, error in spawning?")
                return
            }

            val resInstrumentHolder = BuiltInRegistries.INSTRUMENT
                .getHolder(ResourceKey.create(Registries.INSTRUMENT, GrowssethItems.Instruments.RESEARCHER_HORN.first))
                .orElseThrow()
            val hornItem = InstrumentItem.create(GrowssethItems.RESEARCHER_HORN, resInstrumentHolder)
            hornItem.loreLines().add(Component.translatable("item.growsseth.researcher_horn.description1"))
            hornItem.loreLines().add(Component.translatable("item.growsseth.researcher_horn.description2"))
            val researcherName = ResearcherSavedData.getPersistent(level.server).name  ?: Component.translatable("entity.growsseth.researcher")

            val finalDiaryTemplate = if (!playerSkippedQuest) "quest_good_ending" else "quest_good_ending_skip"
            val endTextItem = (if (DiaryHelper.hasCustomEndDiary()) {
                    DiaryHelper.getCustomEndDiary(researcherName)
                } else {
                    null
                }) ?:BookTemplates.createTemplatedBook(finalDiaryTemplate, edit = { withAuthor(researcherName.string) })
                ?: Items.PAPER.defaultInstance.copyWithCount(1).also { itemStack ->
                    itemStack[DataComponents.CUSTOM_NAME] = Component.translatable("growsseth.final_diary_fallback")
                    RuinsOfGrowsseth.LOGGER.warn("Couldn't load final diary, researcher used a piece of paper instead!")
                }

            blockEntity.setItem(4, endTextItem)
            blockEntity.setItem(13, hornItem)
            level.blockUpdated(pos, chestState.block)
            RuinsOfGrowsseth.LOGGER.info("Spawned researcher reward chest at $pos")
        }
    }

    inner class StartStage : QuestStage<Researcher> {
        override val trigger = ProgressTradesTrigger(server, onlyOne = true)
            .or(ApiEventTrigger(finalQuestStartName))

        override fun onActivated(entity: Researcher) {
            entity.healed = false    // for resetting discounts when using gquest
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    inner class ZombieStage : QuestStage<Researcher> {
        override val trigger = ProgressTradesTrigger(server, onlyOne = false)
            .or(ApiEventTrigger(finalQuestZombieName))

        override fun onActivated(entity: Researcher) {
            entity.healed = false    // for resetting discounts when using gquest
        }

        // Trigger on update too to cover multiple tent situations
        // but run logic at tick end to avoid issues with replacing entities as it sometimes desynced
        // or something, keeping both zombie and researcher
        override fun onUpdate(entity: Researcher) = EventUtil.runAtServerTickEnd { _ ->
            if (entity.dialogues?.getTriggeredDialogues()?.isEmpty() == true)
                playerSkippedQuest = true

            val tent = entity.tent
            val startingPos = entity.position()
            entity.dialogues?.resetNearbyPlayers()
            val data = entity.saveResearcherData()
            var scheduleMoveRemoveLater = false
            if (tent != null) {
                moveToJail(entity, tent)
                createDiary(entity, tent)
                //remove white carpet to show trapdoor
                tent.remove(entity.level() as ServerLevel, GrowssethTags.TENT_CLEAR_ZOMBIE_STAGE_WHITELIST)
            } else {
                scheduleMoveRemoveLater = true
            }
            val level = entity.level() as ServerLevel
            val resStartingPos = entity.startingPos ?: entity.blockPosition()
            val spawnTime = entity.spawnTime

            val zombie = entity.convertTo(GrowssethEntities.ZOMBIE_RESEARCHER, false)
            if (isNull(zombie)) {
                RuinsOfGrowsseth.LOGGER.error("Couldn't zombify researcher in quest stage!")
                entity.moveTo(startingPos)
                return@runAtServerTickEnd
            }
            zombie.researcherData = data
            zombie.lastWorldDataTime = entity.lastWorldDataTime
            zombie.spawnTime = spawnTime
            zombie.researcherOriginalPos = resStartingPos
            // not visible normally other than with entity info mods
            zombie.villagerData = zombie.villagerData.setProfession(VillagerProfession.CARTOGRAPHER).setLevel(5)
            RuinsOfGrowsseth.LOGGER.info("Spawned researcher zombie {} from quest stage", zombie)

            entity.discard()

            // Maybe is now redundant after enclosing the function in serverAtTickEnd, treat this as "next tick end"
            if (scheduleMoveRemoveLater) {
                RuinsOfGrowsseth.LOGGER.info("Couldn't find tent, trying again at end of server tick...")
                // Try moving him again at the end of the tick, maybe this is during load and the structure wasn't
                // properly stored (usually in testing scenarios)
                EventUtil.runAtServerTickEnd { _ ->
                    // entity is zombie by now, likely, and so discarded; use static function instead
                    val tentStart2 = Researcher.findTent(level, resStartingPos)
                    val tent2 = tentStart2?.pieces?.get(0) as ResearcherTent?
                    tent2?.let {
                        it.remove(entity.level() as ServerLevel, GrowssethTags.TENT_CLEAR_ZOMBIE_STAGE_WHITELIST)
                        moveToJail(zombie, it)
                        createDiary(zombie, it)
                        RuinsOfGrowsseth.LOGGER.info("Success in moving at end of server tick")
                    } ?: RuinsOfGrowsseth.LOGGER.warn("Cannot find tent to move zombie researcher to!")
                }
            }
        }

        private fun createDiary(entity: LivingEntity, tent: ResearcherTent) {
            val zombieDiaryTemplate = if (!playerSkippedQuest) "quest_zombie" else "quest_zombie_skip"
            val diary = BookTemplates.createTemplatedBook(zombieDiaryTemplate, edit = { withAuthor(entity.name.string) })
            if (diary == null) {
                RuinsOfGrowsseth.LOGGER.error("No diary for quest_zombie!")
                return
            }

            var lectern: LecternBlockEntity? = null
            var chest: ChestBlockEntity? = null

            tent.cellarBoundingBox?.let { boundingBox ->
                boundingBox.iterBlocks { pos ->
                    val found = serverLevel.getBlockEntity(pos)?.let {
                        if (it is LecternBlockEntity) {
                            lectern = it
                        }
                        if (it is ChestBlockEntity) {
                            chest = it
                        }
                        chest != null && lectern != null
                    } == true
                    if (found) return@iterBlocks
                }
            }

            DiaryHelper.pushDiaryToContainers(diary, serverLevel, entity, lectern, chest)
            RuinsOfGrowsseth.LOGGER.info("Placed diary for quest")
        }

        private fun moveToJail(entity: Entity, tent: ResearcherTent) {
            val pos = tent.jailPos
            if (notNull(pos)) {
                // Move before to hide transition, as this is in parallel with client
                entity.moveTo(pos, entity.yRot, entity.xRot)
            } else {
                RuinsOfGrowsseth.LOGGER.warn("Tent doesn't have a jail position set!")
            }
        }
    }

    class HealedStage: QuestStage<Researcher> {
        // Automatically trigger as soon as healed (and quests work again)
        override val trigger = QuestStageTrigger<Researcher> { entity, _ ->
            entity.healed
        }

        override fun onActivated(entity: Researcher) {
            entity.healed = true    // for discounts when using gquest
        }
    }

    // First this stage where we wait for healed dialogue to play out,
    // AFTER that following stage where it waits for time or reload AFTER the dialogue plays
    // (to ensure npc doesn't immediately teleport after starting healed dialogue if reached after timeout trigger)
    inner class HealedWaitForDialogueStage : QuestStage<Researcher> {
        // Trigger when healed dialogue is triggered
        override val trigger: QuestStageTrigger<Researcher> = DialogueGroupTrigger("group-cure-dialogue")

        override fun onActivated(entity: Researcher) {
            entity.healed = true    // for discounts when using gquest
        }
    }

    inner class HomeLastDialogueStage: QuestStage<Researcher> {
        override val trigger = commonReloadTrigger

        override fun onActivated(entity: Researcher) {
            entity.healed = true    // for discounts when using gquest
            entity.startingPos?.let { entity.moveTo(it, entity.yRot, entity.xRot) }
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    // First wait for researcher to say final dialogue...
    class WaitBeforeLeaveStage : QuestStage<Researcher> {
        override val trigger = DialogueGroupTrigger<Researcher>("group-quest-last-dialogue")

        override fun onActivated(entity: Researcher) {
            entity.healed = true    // for discounts when using gquest
        }
    }

    // ...THEN start counting time
    inner class EndingStage: QuestStage<Researcher> {
        // wait 2 days / manual trigger and reload
        override val trigger: QuestStageTrigger<Researcher> = (
                commonReloadTrigger
                and (
                    TimeOrDayTimeTrigger(this@ResearcherQuestComponent, Constants.DAY_TICKS_DURATION * 2)
                    or ApiEventTrigger(finalQuestLeaveName)
                )
            )

        override fun onActivated(entity: Researcher) {
            entity.healed = true    // for discounts when using gquest
        }

        // OnUpdate to also cover multiple tents
        override fun onUpdate(entity: Researcher) {
            // for testing with gquest, without having to do the zombie stage first
            if (entity.dialogues!!.getTriggeredDialogues().isEmpty())
                playerSkippedQuest = true

            if (!alreadyRemovedTent)
                removeTentAndResearcher(entity)
            else
                removeResearcher(entity)
        }
    }


    private class ProgressTradesTrigger(val server: MinecraftServer, val onlyOne: Boolean = false) : QuestStageTrigger<Researcher> {
        override fun isActive(entity: Researcher, event: QuestUpdateEvent): Boolean {
            val tradesProvider = ResearcherTradeMode.providerFromSettings(server)
            if (tradesProvider !is ProgressResearcherTradesProvider) return false

            return if (onlyOne) tradesProvider.onlyOneLeft(server)
                else tradesProvider.isFinished(server)
        }
    }
}