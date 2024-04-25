package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.EventUtil
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.BoundingBox


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

    fun removeDonkey(entity: Researcher, level: ServerLevel, predicate: ((Entity) -> Boolean)? = null) {
        removeDonkey(entity.persistId ?: throw IllegalStateException("Researcher has no persist id $entity"), level)
    }

    fun removeDonkey(persistId: Int, level: ServerLevel, predicate: ((Entity) -> Boolean)? = null) {
        val worldData = ResearcherSavedData.get(level.server, persistId)
        worldData?.donkeyUuid?.let{ uuid ->
            EventUtil.runOnEntityWhenPossible(level, uuid) {
                if (predicate == null || predicate(it)) {
                    it.discard()
                    RuinsOfGrowsseth.logDev(org.apache.logging.log4j.Level.INFO, "Removed donkey $uuid")
                }
            }
        } ?: RuinsOfGrowsseth.LOGGER.warn("Couldn't find donkey to remove!")
    }
}
