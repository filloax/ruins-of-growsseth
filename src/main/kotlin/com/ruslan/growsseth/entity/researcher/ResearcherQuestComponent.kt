package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.EventUtil
import com.filloax.fxlib.SetBlockFlag
import com.filloax.fxlib.alwaysTruePredicate
import com.filloax.fxlib.codec.decodeNbt
import com.filloax.fxlib.codec.encodeNbt
import com.filloax.fxlib.codec.simpleCodecErr
import com.filloax.fxlib.codec.throwableCodecErr
import com.filloax.fxlib.iterBlocks
import com.filloax.fxlib.nbt.getCompoundOrNull
import com.filloax.fxlib.nbt.getOrPut
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.QuestConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.trades.ProgressResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTradeMode
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.quests.*
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
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
        const val HOME = "home"
        const val WAIT = "wait"
        const val ENDING = "ending"
    }

    // Used to avoid repeating full tent removal with multiple tents in normal worlds
    private var alreadyRemovedTent = false

    init {
        addStage(Stages.START, StartStage())
        // can skip start
        addStage(Stages.ZOMBIE, ZombieStage(), Stages.START, INIT_STAGE_ID, priority = -10, blockSiblingStages = true)
        addStage(Stages.HEALED, HealedStage(), Stages.ZOMBIE)
        addStage(Stages.HOME, LastDialogueStage(), Stages.HEALED, blockNextStages = true)
        addStage(Stages.WAIT, WaitBeforeLeaveStage(), Stages.HOME, blockNextStages = true)
        addStage(Stages.ENDING, EndingStage(), Stages.WAIT)
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
            assert(stage in listOf(Stages.HOME, Stages.WAIT, Stages.START, Stages.HEALED, Stages.ZOMBIE, Stages.ENDING)) { "Stage $stage not included in stages for researcher!" }
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
            researcher.tent?.let { tent ->
                val giftPos = tent.boundingBox.center
                spawnRewardChest(level, giftPos)
                tent.remove(level, replaceUndergroundEntrance = true)
            }
            removeResearcher(researcher)
        }

        fun removeResearcher(researcher: Researcher) {
            val level = researcher.level() as ServerLevel
            if (!researcher.donkeyWasBorrowed) {
                ResearcherDonkey.removeDonkey(researcher, level)
            }
            val savedData = ResearcherSavedData.getPersistent(level.server)
            researcher.writeSavedData(savedData)
            researcher.discard()
        }

        fun spawnRewardChest(level: ServerLevel, pos: BlockPos) {
            val chestState: BlockState = Blocks.CHEST.defaultBlockState()
            level.setBlock(pos, chestState, SetBlockFlag.or(
                SetBlockFlag.NOTIFY_CLIENTS,
                SetBlockFlag.NO_NEIGHBOR_REACTIONS,
                SetBlockFlag.NO_NEIGHBOR_REACTION_DROPS
            ))
            val blockEntity = level.getBlockEntity(pos)
            val resInstrumentHolder = BuiltInRegistries.INSTRUMENT
                .getHolder(ResourceKey.create(Registries.INSTRUMENT, GrowssethItems.Instruments.RESEARCHER_HORN.first))
                .orElseThrow()
            val hornItem = InstrumentItem.create(GrowssethItems.RESEARCHER_HORN, resInstrumentHolder)
            val researcherName = ResearcherSavedData.getPersistent(level.server).name  ?: Component.translatable("entity.growsseth.researcher")

            blockEntity?.loadWithComponents(CompoundTag().also { chestTag ->
                chestTag.put("Items", ListTag().also { items ->
                    val endTextItem = (if (DiaryHelper.hasCustomEndDiary()) {
                        DiaryHelper.getCustomEndDiary(researcherName)
                    } else {
                        null
                    }) ?: DiaryHelper.createMiscDiary("quest_good_ending", researcherName)
                    ?: Items.PAPER.defaultInstance.copyWithCount(1).also { itemStack ->
                        itemStack[DataComponents.CUSTOM_NAME] = Component.literal("Per il mio collega")
                    }

                    items.add(endTextItem.save(level.registryAccess(), CompoundTag().also{ it.putByte("Slot", 4) }))
                    items.add(hornItem.save(level.registryAccess(), CompoundTag().also{ it.putByte("Slot", 13) }))
                })
            }, level.registryAccess()) ?: RuinsOfGrowsseth.LOGGER.error("No blockentity at reward chest pos $pos, error in spawning?")
        }
    }

    inner class StartStage : QuestStage<Researcher> {
        override val trigger = ProgressTradesTrigger(server, onlyOne = true)
            .or(ApiEventTrigger(QuestConfig.finalQuestStartName))


        override fun onActivated(entity: Researcher) {
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    inner class ZombieStage : QuestStage<Researcher> {
        override val trigger = ProgressTradesTrigger(server, onlyOne = false)
            .or(ApiEventTrigger(QuestConfig.finalQuestZombieName))

        // Trigger on update too to cover multiple tent situations
        override fun onUpdate(entity: Researcher) {
            val tent = entity.tent
            val startingPos = entity.position()
            entity.dialogues?.resetNearbyPlayers()
            val data = entity.makeResearcherData()
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

            entity.saveWorldData()
            val zombie = entity.convertTo(GrowssethEntities.ZOMBIE_RESEARCHER, false)
            if (zombie == null) {
                RuinsOfGrowsseth.LOGGER.error("Couldn't zombify researcher in quest stage!")
                entity.moveTo(startingPos)
                return
            }
            zombie.villagerData = zombie.villagerData.setProfession(VillagerProfession.CARTOGRAPHER).setLevel(5)
            zombie.researcherData = data
            zombie.spawnTime = spawnTime
            zombie.researcherOriginalPos = resStartingPos

            entity.discard()

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

        private fun createDiary(entity: Entity, tent: ResearcherTent) {
            val diary = DiaryHelper.createMiscDiary("quest_zombie", entity)
            if (diary == null) {
                RuinsOfGrowsseth.LOGGER.error("No diary for quest_zombie!")
                return
            }

            var lectern: LecternBlockEntity? = null
            var chest: ChestBlockEntity? = null

            tent.cellarBoundingBox?.let { boundingBox ->
                for (pos in iterBlocks(boundingBox)) {
                    val found = serverLevel.getBlockEntity(pos)?.let {
                        if (it is LecternBlockEntity) {
                            lectern = it
                        }
                        if (it is ChestBlockEntity) {
                            chest = it
                        }
                        chest != null && lectern != null
                    } == true
                    if (found) break
                }
            }

            DiaryHelper.pushDiaryToContainers(diary, serverLevel, entity, lectern, chest)
            RuinsOfGrowsseth.LOGGER.info("Placed diary for quest")
        }

        private fun moveToJail(entity: Entity, tent: ResearcherTent) {
            val pos = tent.jailPos
            if (pos != null) {
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
    }

    // Separate stage for last dialogue, so we can in next stage count
    // time only after dialogue of this quest triggered
    class LastDialogueStage: QuestStage<Researcher> {
        // Automatically trigger as soon as healed (and quests work again)
        override val trigger = EventTrigger<Researcher>(QuestUpdateEvent.LOAD)
            // You can find the dialogue in the quest dialogues json
            .and(DialogueTrigger("researcher-quest-cure"))

        override fun onActivated(entity: Researcher) {
            entity.startingPos?.let { entity.moveTo(it, entity.yRot, entity.xRot) }
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    class WaitBeforeLeaveStage : QuestStage<Researcher> {
        override val trigger = DialogueTrigger<Researcher>("researcher-quest-end")
    }

    inner class EndingStage: QuestStage<Researcher> {
        override val trigger: QuestStageTrigger<Researcher> = EventTrigger<Researcher>(QuestUpdateEvent.LOAD)
            .and(TimeTrigger(this@ResearcherQuestComponent, Constants.DAY_TICKS_DURATION)
                .or(ApiEventTrigger(QuestConfig.finalQuestLeaveName))
            )

        // OnUpdate to also cover multiple tents
        override fun onUpdate(entity: Researcher) {
            if (!alreadyRemovedTent)
                removeTentAndResearcher(entity)
            else
                removeResearcher(entity)
        }
    }

    class ProgressTradesTrigger(val server: MinecraftServer, val onlyOne: Boolean = false) : QuestStageTrigger<Researcher> {
        override fun isActive(entity: Researcher, event: QuestUpdateEvent): Boolean {
            val tradesProvider = ResearcherTradeMode.providerFromSettings(server)
            if (tradesProvider !is ProgressResearcherTradesProvider) return false

            return if (onlyOne) tradesProvider.onlyOneLeft(server)
                else tradesProvider.isFinished(server)
        }
    }
}