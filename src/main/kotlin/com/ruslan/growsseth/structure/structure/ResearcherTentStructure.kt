package com.ruslan.growsseth.structure.structure

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstapContext
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructurePiece
import net.minecraft.world.level.levelgen.structure.StructureType
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment
import java.util.*


class ResearcherTentStructure(
    structureSettings: StructureSettings,
    val templatePath: ResourceLocation = ResearcherTent.DEFAULT_ID,
    val offsetY: Int = -BASEMENT_HEIGHT, // centered on floor height
    forcePosUseY: Boolean = true,
) : SimpleStructure(structureSettings, forcePosUseY) {
    companion object {
        @JvmStatic
        val CODEC: Codec<ResearcherTentStructure> = ExtraCodecs.validate(
            RecordCodecBuilder.mapCodec<ResearcherTentStructure> { builder ->
                builder.group(
                    settingsCodec(builder),
                    Codec.BOOL.optionalFieldOf("force_pos_uses_y").forGetter{ Optional.of(it.nextPlaceUseY) },
                    ResourceLocation.CODEC.optionalFieldOf("templatePath", ResearcherTent.DEFAULT_ID).forGetter{ it.templatePath },
                    Codec.INT.optionalFieldOf("offsetY", -BASEMENT_HEIGHT).forGetter{ it.offsetY },
                ).apply(builder) { structureSettings, forcePosUseY, templatePath, offsetY ->
                    ResearcherTentStructure(
                        structureSettings,
                        templatePath,
                        offsetY,
                        forcePosUseY.orElse(false),
                    )
                }
            }) { structure -> DataResult.success(structure) }.codec()


        // Taken from nbt, so more related to structure piece, but shouldn't change here
        @JvmStatic
        val SIZE = Vec3i(15, 18, 20) // As taken from nbt
        const val BASEMENT_HEIGHT = 10

        fun build(ctx: BootstapContext<Structure>, templatePath: ResourceLocation = ResearcherTent.DEFAULT_ID, offsetY: Int = -BASEMENT_HEIGHT): ResearcherTentStructure {
            return ResearcherTentStructure(StructureSettings(
                ctx.lookup(Registries.BIOME).getOrThrow(TagKey.create(Registries.BIOME, ResourceLocation("growsseth:has_structure/researcher_tent"))),
                mapOf(),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.BEARD_THIN,
            ), templatePath, offsetY)
        }
    }

    override fun getFirstPiece(context: GenerationContext, random: RandomSource, chunkPos: ChunkPos, pos: BlockPos, rotation: Rotation): StructurePiece {
        val offset = BlockPos(-SIZE.x/2, 0, -8).rotate(rotation)    // offset from trial and error
        return ResearcherTent(context.structureTemplateManager, BlockPos(pos.x + offset.x, pos.y + offsetY, pos.z + offset.z), rotation = rotation, templatePath)
    }

    override fun type(): StructureType<*> = GrowssethStructures.Types.RESEARCHER_TENT

    override val nextPlaceUseY = forcePosUseY
}