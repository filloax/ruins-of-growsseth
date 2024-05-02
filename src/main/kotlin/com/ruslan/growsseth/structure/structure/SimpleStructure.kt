package com.ruslan.growsseth.structure.structure

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.filloax.fxlib.*
import com.filloax.fxlib.chunk.isBlockPosInChunk
import com.filloax.fxlib.structure.FixablePosition
import com.filloax.fxlib.structure.FixableRotation
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructurePiece
import java.util.*

// Source for some of this: Twilight Forest Fabric
abstract class SimpleStructure(
    structureSettings: StructureSettings,
    val defaultForcePosUsesY: Boolean = true,
) : Structure(structureSettings), FixablePosition, FixableRotation {
    private var forcePosUsesY = defaultForcePosUsesY
    private var nextPlacePos: BlockPos? = null
    private var nextPlaceRotation: Rotation? = null

    override fun findGenerationPoint(context: GenerationContext): Optional<GenerationStub> {
        val chunkPos = context.chunkPos()
        val curNextPlaceRotation = nextPlaceRotation
        val curForcePosUsesY = forcePosUsesY
        forcePosUsesY = defaultForcePosUsesY

        if (nextPlacePos?.let { !isBlockPosInChunk(chunkPos, it) } == true) {
            RuinsOfGrowsseth.LOGGER.error("Error: fixed position $nextPlacePos not in chunk at $chunkPos")
            nextPlacePos = null
        }

        val x = nextPlacePos?.x ?: ((chunkPos.x shl 4) + 7)
        val z = nextPlacePos?.z ?: ((chunkPos.z shl 4) + 7)
        val y = if (curForcePosUsesY && nextPlacePos != null) nextPlacePos!!.y else this.adjustForTerrain(context, x, z)
        val pos = BlockPos(x, y, z)

        val rand = RandomSource.create(context.seed() + chunkPos.x * 25117L + chunkPos.z * 151121L)

        val rotation = curNextPlaceRotation ?: Rotation.getRandom(rand)

        nextPlacePos = null
        nextPlaceRotation = null

        return Optional
            .ofNullable(this.getFirstPiece( context, rand, chunkPos, pos, rotation))
            .map { piece -> getStructurePieceGenerationStubFunction(piece, context, pos) }
    }

    protected abstract fun getFirstPiece(context: GenerationContext, random: RandomSource, chunkPos: ChunkPos, pos: BlockPos, rotation: Rotation): StructurePiece?

    private fun getStructurePieceGenerationStubFunction(startingPiece: StructurePiece, context: GenerationContext, pos: BlockPos): GenerationStub {
        return GenerationStub(pos) { structurePiecesBuilder ->
            structurePiecesBuilder.addPiece(startingPiece)
            startingPiece.addChildren(startingPiece, structurePiecesBuilder, context.random())
        }
    }

    open fun adjustForTerrain(context: GenerationContext, x: Int, z: Int): Int {
        return if (this.shouldAdjustToTerrain())
                context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, context.heightAccessor(), context.randomState())
            else context.chunkGenerator().seaLevel
    }

    protected open fun shouldAdjustToTerrain(): Boolean = true

    override fun setNextPlacePosition(pos: BlockPos, useY: Boolean?) {
        nextPlacePos = pos
        forcePosUsesY = useY ?: defaultForcePosUsesY
    }
    override val nextPlaceUseY: Boolean = false

    override fun setNextPlaceRotation(rotation: Rotation) {
        nextPlaceRotation = rotation
    }
    override val defaultRotation: Rotation? = null
}