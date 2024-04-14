package com.ruslan.growsseth.structure.pieces

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
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
        id: ResourceLocation, settings: StructurePlaceSettings, startPosition: BlockPos,
    ) : super(
        structurePieceType, genDepth, structureManager,
        id, id.toString(), settings, startPosition,
    )

    constructor(structurePieceType: StructurePieceType, compoundTag: CompoundTag, ctx: StructurePieceSerializationContext, settings: StructurePlaceSettings)
        : super(structurePieceType, compoundTag, ctx.structureTemplateManager, { settings })

    protected fun <T : Mob> placeEntity(entityType: EntityType<T>, pos: BlockPos, levelAccessor: ServerLevelAccessor, after: (T) -> Unit = {}) {
        val mob = entityType.create(levelAccessor.level) ?: return
        mob.setPersistenceRequired()
        mob.moveTo(pos.x + .5, pos.y + .0, pos.z + .5, 0.0f, 0.0f)
        mob.finalizeSpawn(levelAccessor, levelAccessor.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null)
        levelAccessor.addFreshEntityWithPassengers(mob)
        levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)
        after(mob)
    }
}