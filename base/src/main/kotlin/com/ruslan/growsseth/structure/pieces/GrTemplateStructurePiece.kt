package com.ruslan.growsseth.structure.pieces

import com.filloax.fxlib.api.enums.SetBlockFlag
import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager


abstract class GrTemplateStructurePiece : TemplateStructurePiece {
    companion object {
        @JvmStatic
        protected fun defaultSettings() : StructurePlaceSettings {
            return StructurePlaceSettings()
//                .addProcessor(BlockIgnoreProcessor(listOf(Blocks.STRUCTURE_VOID))) // Not needed anymore
        }
    }

    constructor(
        structurePieceType: StructurePieceType, genDepth: Int, structureManager: StructureTemplateManager,
        id: Identifier, settings: StructurePlaceSettings, startPosition: BlockPos,
    ) : super(
        structurePieceType, genDepth, structureManager,
        id, id.toString(), settings, startPosition,
    )

    constructor(structurePieceType: StructurePieceType, compoundTag: CompoundTag, ctx: StructurePieceSerializationContext, settings: StructurePlaceSettings)
        : super(structurePieceType, compoundTag, ctx.structureTemplateManager, { settings })

    protected fun <T : Mob> placeEntity(entityType: EntityType<T>, pos: BlockPos, levelAccessor: ServerLevelAccessor, after: (T) -> Unit = {}) {
        try {
            val mob = entityType.create(levelAccessor.level, EntitySpawnReason.MOB_SUMMONED) ?: return
            mob.setPersistenceRequired()
            mob.snapTo(pos.x + .5, pos.y + .0, pos.z + .5, 0.0f, 0.0f)
            mob.finalizeSpawn(levelAccessor, levelAccessor.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null)
            levelAccessor.addFreshEntityWithPassengers(mob)
            levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), SetBlockFlag.NOTIFY_CLIENTS.flag)
            after(mob)
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error(e)
        }
    }
}