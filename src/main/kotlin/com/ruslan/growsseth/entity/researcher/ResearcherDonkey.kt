package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.EventUtil
import com.filloax.fxlib.entity.getPersistData
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.horse.Donkey
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.StructureStart
import net.minecraft.world.phys.AABB


object ResearcherDonkey {
    @JvmStatic
    fun shouldProtectDonkey(level: Level, entity: Entity): Boolean {
        if (!level.isClientSide()) {
            val pos = entity.blockPosition()
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

    fun onFenceUnleash(mob: Mob, pos: BlockPos) {
        if (!(mob is Donkey && mob.tags.contains(Constants.TAG_RESEARCHER_DONKEY))) return

        val playerRadius = 15.0
        // We're getting the player, as no easy way to make this event return it
        val player = (mob.level() as ServerLevel).getNearestPlayer(mob, playerRadius) as ServerPlayer?
            ?: return
        val searchRadius = 80.0
        val searchArea = AABB.ofSize(player.position(), searchRadius, searchRadius, searchRadius)
        val researchers = player.level().getEntitiesOfClass(Researcher::class.java, searchArea)

        researchers.forEach { researcher ->
            if (!researcher.donkeyWasBorrowed) {
                researcher.donkeyWasBorrowed = true
                researcher.dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_BORROW_DONKEY)
            }
        }

        mob.tags.remove(Constants.TAG_RESEARCHER_DONKEY)
    }

    fun onFenceLeash(mob: Mob, pos: BlockPos, player: ServerPlayer) {
        if (!(mob is Donkey && !mob.tags.contains(Constants.TAG_RESEARCHER_DONKEY))) return

        val serverLevel = player.serverLevel()

        val tent = Researcher.findTent(serverLevel, pos) ?: return
        val researchers = serverLevel.getEntitiesOfClass(Researcher::class.java, AABB.of(tent.boundingBox).inflate(80.0))

        researchers.forEach { researcher ->
            if (researcher.donkeyWasBorrowed) {
                researcher.donkeyWasBorrowed = false
                researcher.dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_RETURN_DONKEY)
            }
        }
    }

    fun removeDonkey(entity: Researcher, level: ServerLevel, predicate: ((Entity) -> Boolean) = {true}) {
        val tent = entity.tent ?: return
        removeDonkey(tent, level, predicate)
    }

    fun removeDonkey(tent: ResearcherTent, level: ServerLevel, predicate: ((Entity) -> Boolean) = {true}) {
        val donkeys = level.getEntitiesOfClass(Researcher::class.java, AABB.of(tent.boundingBox).inflate(80.0), predicate)
        val noItemDonkey = donkeys.filter { it.inventory.isEmpty }.firstOrNull()
        if (noItemDonkey == null && donkeys.isNotEmpty()) {
            RuinsOfGrowsseth.LOGGER.warn("Couldn't remove donkey as inventory was not empty")
        }
        if (noItemDonkey != null) {
            RuinsOfGrowsseth.LOGGER.info("Removing Researcher donkey $noItemDonkey")
            noItemDonkey.discard()
        }
    }
}
