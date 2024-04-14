package com.ruslan.growsseth.structure.pieces

import com.ruslan.growsseth.structure.GrowssethStructurePieceTypes
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager


class SimpleStructurePiece : GrTemplateStructurePiece {
    constructor(structureManager: StructureTemplateManager, startPosition: BlockPos, templatePath: ResourceLocation, rotation: Rotation = Rotation.NONE)
        : super(
            GrowssethStructurePieceTypes.SIMPLE, GEN_DEPTH, structureManager,
            templatePath, makeSettings(rotation), startPosition,
        )

    constructor(compoundTag: CompoundTag, ctx: StructurePieceSerializationContext)
        : super(GrowssethStructurePieceTypes.SIMPLE, compoundTag, ctx, makeSettings(Rotation.valueOf(compoundTag.getString("Rot"))))

    companion object {
        const val GEN_DEPTH = 0

        fun makeSettings(rotation: Rotation): StructurePlaceSettings
            = defaultSettings().setRotation(rotation)
    }

    override fun addAdditionalSaveData(context: StructurePieceSerializationContext, tag: CompoundTag) {
        super.addAdditionalSaveData(context, tag)
        tag.putString("Rot", placeSettings.rotation.name)
    }

    override fun handleDataMarker(
        name: String,
        pos: BlockPos,
        level: ServerLevelAccessor,
        random: RandomSource,
        box: BoundingBox,
    ) {

    }
}