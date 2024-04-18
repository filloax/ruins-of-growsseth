package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.EventUtil
import com.filloax.fxlib.SetBlockFlag
import com.filloax.fxlib.iterBlocks
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.QuestConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.quests.*
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
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
 * ending: Chunk reloaded: vanish
 */
class ResearcherQuestComponent(researcher: Researcher) : QuestComponent<Researcher>(researcher, "researcherIllness") {
    init {
        addStage("start", StartStage())
        // can skip start
        addStage("zombie", ZombieStage(), "start", INIT_STAGE_ID, priority = -10, blockSiblingStages = true)
        addStage("healed", HealedStage(), "zombie")
        addStage("home", MoveBackToTentStage(), "healed", blockNextStages = true)
        addStage("ending", EndingStage(), "home")
    }

    class StartStage : QuestStage<Researcher> {
        override val trigger = ApiEventTrigger<Researcher>(QuestConfig.finalQuestStartName)

        override fun onActivated(entity: Researcher) {
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    inner class ZombieStage : QuestStage<Researcher> {
        override val trigger = ApiEventTrigger<Researcher>(QuestConfig.finalQuestZombieName)

        override fun onActivated(entity: Researcher) {
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

        override fun onActivated(entity: Researcher) {
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    class MoveBackToTentStage: QuestStage<Researcher> {
        // Automatically trigger as soon as healed (and quests work again)
        override val trigger = EventTrigger<Researcher>(QuestUpdateEvent.LOAD)
            // You can find the dialogue in the quest dialogues json
            .and(DialogueTrigger("researcher-quest-cure"))

        override fun onActivated(entity: Researcher) {
            entity.startingPos?.let { entity.moveTo(it, entity.yRot, entity.xRot) }
            entity.dialogues?.resetNearbyPlayers()
        }
    }

    class EndingStage: QuestStage<Researcher> {
        override val trigger: QuestStageTrigger<Researcher> = ApiEventTrigger<Researcher>(QuestConfig.finalQuestLeaveName)
            .and(EventTrigger(QuestUpdateEvent.LOAD))

        override fun onActivated(entity: Researcher) {
            removeTentAndResearcher(entity)
        }
    }

    companion object {
        fun removeTentAndResearcher(researcher: Researcher) {
            val level = researcher.level() as ServerLevel
            researcher.tent?.let { tent ->
                val giftPos = tent.boundingBox.center
                spawnRewardChest(level, giftPos, researcher.persistId)
                tent.remove(level, replaceUndergroundEntrance = true)
            }
            if (!researcher.donkeyWasBorrowed) {
                ResearcherDonkey.removeDonkey(researcher, level)
            }
            researcher.discard()
        }

        fun spawnRewardChest(level: ServerLevel, pos: BlockPos, persistId: Int?) {
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
            val researcherName = persistId?.let { ResearcherSavedData.get(level.server, it)?.name } ?: Component.translatable("entity.growsseth.researcher")

            blockEntity?.load(CompoundTag().also { chestTag ->
                chestTag.put("Items", ListTag().also { items ->
                    val endTextItem = (if (DiaryHelper.hasCustomEndDiary()) {
                        DiaryHelper.getCustomEndDiary(researcherName)
                    } else {
                        null
                    }) ?: DiaryHelper.createMiscDiary("quest_good_ending", researcherName)
                        ?: Items.PAPER.defaultInstance.copyWithCount(1).also { itemStack ->
                            itemStack.setHoverName(Component.literal("Per il mio collega"))
                        }

                    items.add(endTextItem
                        .save(CompoundTag().also{ it.putInt("Slot", 4) }))
                    items.add(hornItem
                        .save(CompoundTag().also{ it.putInt("Slot", 13) }))
                })
            }) ?: RuinsOfGrowsseth.LOGGER.error("No blockentity at reward chest pos $pos, error in spawning?")
        }
    }
}