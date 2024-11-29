package com.ruslan.growsseth.advancements.criterion

import com.filloax.fxlib.api.codec.constructorWithOptionals
import com.filloax.fxlib.api.codec.forNullableGetter
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.utils.matches
import net.minecraft.advancements.critereon.BlockPredicate
import net.minecraft.advancements.critereon.FluidPredicate
import net.minecraft.advancements.critereon.LightPredicate
import net.minecraft.advancements.critereon.LocationPredicate.PositionPredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureType
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement

data class JigsawPiecePredicate(
    val structure: ResourceKey<Structure>,
    val jigsawPieceIds: List<ResourceLocation>,
    val position: PositionPredicate? = null,
    val biome: ResourceKey<Biome>? = null,
    val dimension: ResourceKey<Level>? = null,
    val smokey: Boolean? = null,
    val light: LightPredicate? = null,
    val block: BlockPredicate? = null,
    val fluid: FluidPredicate? = null,
) {
    companion object {
        val CODEC: Codec<JigsawPiecePredicate> = RecordCodecBuilder.create { builder -> builder.group(
            ResourceKey.codec(Registries.STRUCTURE).fieldOf("structure").forGetter(JigsawPiecePredicate::structure),
            ResourceLocation.CODEC.listOf().fieldOf("jigsawPieceIds").forGetter(JigsawPiecePredicate::jigsawPieceIds),
            PositionPredicate.CODEC.optionalFieldOf("position").forNullableGetter(JigsawPiecePredicate::position),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forNullableGetter(JigsawPiecePredicate::biome),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forNullableGetter(
                JigsawPiecePredicate::dimension),
            Codec.BOOL.optionalFieldOf("smokey").forNullableGetter(JigsawPiecePredicate::smokey),
            LightPredicate.CODEC.optionalFieldOf("light").forNullableGetter(JigsawPiecePredicate::light),
            BlockPredicate.CODEC.optionalFieldOf("block").forNullableGetter(JigsawPiecePredicate::block),
            FluidPredicate.CODEC.optionalFieldOf("fluid").forNullableGetter(JigsawPiecePredicate::fluid),
        ).apply(builder, JigsawPiecePredicate::class.constructorWithOptionals()::newInstance) }
    }

    fun matches(level: ServerLevel, x: Double, y: Double, z: Double): Boolean {
        checkJigsaw(level.registryAccess())

        this.position?.let{ if (!it.matches(x, y, z)) return false }
        this.dimension?.let{ if (it != level.dimension()) return false }

        val blockPos = BlockPos.containing(x, y, z)

        this.biome?.let { if (!level.getBiome(blockPos).`is`(it)) return false }
        if (!level.isLoaded(blockPos)) return false

        val structureObj = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(structure)
            .map{it.value()}.or { throw IllegalStateException("Unknown structure $structure") }.orElseThrow()
        val structureStart = level.structureManager().getStructureWithPieceAt(blockPos, structureObj)
        if (!structureStart.isValid) return false

        smokey?.let { if (CampfireBlock.isSmokeyPos(level, blockPos) != it) return false }
        light?.let { if (!it.matches(level, blockPos)) return false }
        block?.let { if (!it.matches(level, blockPos)) return false }
        fluid?.let { if (!it.matches(level, blockPos)) return false }

        // Above here is equivalent to LocationPredicate, next is new logic

        val pieces = structureStart.pieces
        pieces.forEach { piece ->
            if (piece is PoolElementStructurePiece && piece.boundingBox.isInside(blockPos) && piece.element.matches(jigsawPieceIds)) {
                return true
            }
        }
        return false
    }

    private var checked = false

    private fun checkJigsaw(registryAccess: RegistryAccess) {
        if (checked) return

        val structureVal = registryAccess.registryOrThrow(Registries.STRUCTURE).getOrThrow(structure)
        if (structureVal.type() != StructureType.JIGSAW) {
            throw IllegalStateException("Structure in JigsawPiecePredicate is not a jigsaw! Is $structure")
        }
        checked = true
    }
}