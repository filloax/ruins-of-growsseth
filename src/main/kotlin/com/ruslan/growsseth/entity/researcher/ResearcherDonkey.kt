package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.EventUtil
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.horse.Donkey
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB


object ResearcherDonkey {
    @JvmStatic
    fun shouldProtectDonkey(level: Level, donkey: Entity): Boolean {
        if (!level.isClientSide()) {
            // large bounding box because there are already other checks
            val nearbyResearchers = level.getEntitiesOfClass(Researcher::class.java, AABB.ofSize(donkey.position(), 80.0, 80.0, 80.0))
            if (nearbyResearchers.size == 0)
                return false

            val pos = donkey.blockPosition()
            val serverLevel = level as ServerLevel
            val structures = serverLevel.structureManager()

            // Check if position or nearby positions (only checking corners for simplicity) are inside tent
            val checkArea = BoundingBox(pos.x - 3, pos.y - 2, pos.z - 3, pos.x + 3, pos.y + 2, pos.z + 3)
            val checkPoints = mutableListOf(pos).also { checkArea.forAllCorners(it::add) }

            for (checkPos in checkPoints) {
                if (structures.getStructureWithPieceAt(checkPos, GrowssethTags.StructTags.RESEARCHER_TENT).isValid) {
                    return true
                }
            }
        }
        return false
    }

    fun onFenceUnleash(mob: Leashable, pos: BlockPos) {
        if (!(mob is Donkey && mob.tags.contains(Constants.TAG_RESEARCHER_DONKEY))) return

        // We're getting the player, as no easy way to make this event return it
        val playerRadius = 15.0
        val player = (mob.level() as ServerLevel).getNearestPlayer(mob, playerRadius) as ServerPlayer?
            ?: return

        val searchRadius = 80.0
        val searchArea = AABB.ofSize(player.position(), searchRadius, searchRadius, searchRadius)
        val researchers = player.level().getEntitiesOfClass(Researcher::class.java, searchArea)

        researchers.forEach { researcher ->
            if (!researcher.donkeyWasBorrowed) {
                researcher.donkeyWasBorrowed = true
                if (researcher.healed)      // different dialogue since there is no penalty after healing researcher
                    researcher.dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_BORROW_DONKEY_HEALED)
                else
                    researcher.dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_BORROW_DONKEY)
            }
        }
        mob.tags.remove(Constants.TAG_RESEARCHER_DONKEY)
    }

    fun onFenceLeash(mob: Leashable, pos: BlockPos, player: ServerPlayer) {
        if (!(mob is Donkey && !mob.tags.contains(Constants.TAG_RESEARCHER_DONKEY))) return

        val serverLevel = player.serverLevel()

        val tent = Researcher.findTent(serverLevel, pos) ?: return
        val researchers = serverLevel.getEntitiesOfClass(Researcher::class.java, AABB.of(tent.boundingBox).inflate(80.0))

        researchers.forEach { researcher ->
            if (researcher.donkeyWasBorrowed) {
                researcher.donkeyWasBorrowed = false
                if (!researcher.healed)
                    researcher.dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_RETURN_DONKEY)
            }
        }
        mob.tags.add(Constants.TAG_RESEARCHER_DONKEY)
    }

    fun removeDonkey(entity: Researcher, level: ServerLevel, predicate: ((Entity) -> Boolean) = {true}) {
        val tent = entity.tent ?: return
        removeDonkey(tent, level, predicate)
    }

    fun removeDonkey(tent: ResearcherTent, level: ServerLevel, predicate: ((Entity) -> Boolean) = {true}) {
        val donkeys = level.getEntitiesOfClass(Donkey::class.java, AABB.of(tent.boundingBox).inflate(80.0), predicate)
//        val noItemDonkey = donkeys.firstOrNull { !hasItems(it) }
//        if (noItemDonkey == null && donkeys.isNotEmpty()) {
//            RuinsOfGrowsseth.LOGGER.warn("Couldn't remove donkey as inventory was not empty")
//        }
        val useDonkey = donkeys.firstOrNull()
        if (useDonkey != null) {
            RuinsOfGrowsseth.LOGGER.info("Removing Researcher donkey $useDonkey")

            val knot = useDonkey.leashHolder?.let { if (it is LeashFenceKnotEntity) it else null }

            EventUtil.runAtServerTickEnd {
                useDonkey.discard()
                knot?.discard()
            }
        }
    }
}
