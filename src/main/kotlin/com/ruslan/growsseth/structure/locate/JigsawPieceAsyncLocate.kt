package com.ruslan.growsseth.structure.locate

import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.RuinsOfGrowsseth
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.SectionPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.structure.*
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

data class JigsawLocateResult(
    val pos: BlockPos,
    val structure: Holder<Structure>,
)

typealias JigsawSignalProgressFun = (JigsawLocateTask, JigsawLocateTask.Phase, Float) -> Unit

/**
 * Exploit the fact jigsaws generate the pieces before spawning to check
 * if a structure will contain a specific piece or pieces
 */
class JigsawLocateTask(
    private val chunkGenerator: ChunkGenerator,
    val level: ServerLevel,
    val fromPos: BlockPos,
    val targetSet: HolderSet<Structure>,
    val targetPieceIds: Set<ResourceLocation>,
    val searchRadius: Int,
    val skipKnownStructures: Boolean,
    private val signalProgress: JigsawSignalProgressFun?
) {
    private val server = level.server
    private lateinit var startTime: Instant
    private val isCancelled = atomic<Boolean>(false)
    private val cancelReason = atomic<String?>(null)
    private val finalTimeMs = atomic<Long?>(null)
    private val future = CompletableFuture<JigsawLocateResult?>()
    private val done_ = atomic<Boolean>(false)
    val done get() = done_.value

    fun start() {
        startTime = Clock.System.now()

        if (!targetSet.all { it.value() is JigsawStructure }) {
            RuinsOfGrowsseth.LOGGER.error("Cannot search for jigsaws as not all target structures are jigsaws: $targetSet")
            onCancel()
            return
        }

        try {
            RuinsOfGrowsseth.LOGGER.info(
                "[jigsaw] Trying to locate {} in {} around {} within {} chunks",
                targetString(), level, fromPos, searchRadius
            )

            val result = findNearestMatchingJigsaw()
            val success = result.first
            if (success) {
                onFinish(result.second)
            } else {
                onCancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            future.complete(null)
        } finally {
            done_.update { true }
        }
    }

    fun then(action: (JigsawLocateResult?) -> Unit): JigsawLocateTask {
        future.thenAccept(action)
        return this
    }

    fun thenOnServerThread(action: (JigsawLocateResult?) -> Unit): JigsawLocateTask {
        future.thenAccept{ result -> server.submit{ simpleTryCatch{ action(result) } } }
        return this
    }

    fun onException(action: (e: Throwable) -> JigsawLocateResult?) {
        future.exceptionally(action)
    }

    fun onExceptionOnServerThread(action: (e: Throwable) -> Unit) {
        future.exceptionally { e -> server.submit { simpleTryCatch { action(e) } }; null }
    }

    // For situations that would otherwise silently ignore exceptions
    private fun <T> simpleTryCatch(action: () -> T?): T? {
        return try {
            action()
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun timeElapsedMs() = (Clock.System.now() - startTime).inWholeMilliseconds

    fun cancel(reason: String? = null) {
        RuinsOfGrowsseth.LOGGER.warn("Stopping async locate for jigsaw ${targetString()} from pos $fromPos after ${timeElapsedMs() / 1000.0}s, " +
                (if (reason != null) "reason: $reason, " else "") +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        cancelReason.update { reason }
        isCancelled.update { true }
    }

    /**
     * Code analoguous to base [ChunkGenerator.findNearestMapStructure],
     * but with added checks and signals
     */
    private fun findNearestMatchingJigsaw(): Pair<Boolean, JigsawLocateResult?> {
        val structureState = level.chunkSource.generatorState
        val placementsSets: MutableMap<StructurePlacement, MutableSet<Holder<Structure>>> = Object2ObjectArrayMap()

        for (holder in targetSet) {
            for (structurePlacement in structureState.getPlacementsForStructure(holder)) {
                placementsSets.computeIfAbsent(structurePlacement) { ObjectArraySet() }.add(holder)
            }
        }

        if (placementsSets.isEmpty()) {
            return Pair(true, null)
        } else {
            var output: JigsawLocateResult? = null
            var currentDistance = Double.MAX_VALUE
            val structureManager = level.structureManager()
            val randomSpreadStructures: MutableList<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> = ArrayList(placementsSets.size)
            val concentricRingsStructures: MutableList<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> = ArrayList(placementsSets.size)

            for (entry in placementsSets.entries) {
                val key = entry.key
                if (key is ConcentricRingsStructurePlacement) {
                    concentricRingsStructures.add(entry)
                } else if (key is RandomSpreadStructurePlacement) {
                    randomSpreadStructures.add(entry)
                }
            }

            if (concentricRingsStructures.isNotEmpty()) {
                RuinsOfGrowsseth.LOGGER.warn("Concentric rings jigsaw placement NYI! (Not needed as currently only done for villages which are randomspread)")
                /*
                val structuresSize = concentricRingsStructures.size
                for ((i, entry) in concentricRingsStructures.withIndex()) {
                    if (shouldStop()) {
                        return Pair(false, null)
                    }

                    val structurePlacement = entry.key as ConcentricRingsStructurePlacement
                    val ringsResult: Pair<BlockPos, Holder<Structure>>? = chunkGenerator.getNearestGeneratedStructure(
                        entry.value, level, structureManager, fromPos, skipKnownStructures,
                        structurePlacement
                    )
                    if (ringsResult != null) {
                        val foundPos = ringsResult.first
                        val dist = fromPos.distSqr(foundPos)
                        if (dist < currentDistance) {
                            currentDistance = dist
                            output = ringsResult
                        }
                    }

                    signalProgress?.let { it(this, Phase.CONCENTRIC_RINGS, (i.toFloat() + 1) / structuresSize) }
                }
                */
            }

            if (randomSpreadStructures.isNotEmpty()) {
                val sectionX = SectionPos.blockToSectionCoord(fromPos.x)
                val sectionZ = SectionPos.blockToSectionCoord(fromPos.z)
                val searchSize = randomSpreadStructures.size * (searchRadius + 1)
                var i = 0f

                for (currentSearchRadius in 0..searchRadius) {
                    var found = false

                    for (entry2 in randomSpreadStructures) {
                        if (shouldStop()) {
                            return Pair(false, null)
                        }

                        val randomSpreadStructurePlacement = entry2.key as RandomSpreadStructurePlacement
                        val spreadResult = getNearestGeneratedStructureSpread(
                            entry2.value,
                            level,
                            structureManager,
                            sectionX,
                            sectionZ,
                            currentSearchRadius,
                            skipKnownStructures,
                            structureState.levelSeed,
                            randomSpreadStructurePlacement
                        )
                        if (spreadResult != null) {
                            found = true
                            val dist = fromPos.distSqr(spreadResult.pos)
                            if (dist < currentDistance) {
                                currentDistance = dist
                                output = spreadResult
                            }
                        }

                        i++
                        signalProgress?.let { it(this, Phase.RANDOM_SPREAD, i / searchSize) }
                    }

                    if (found) {
                        return Pair(true, output)
                    }
                }
            }

            return Pair(true, output)
        }
    }

    private fun getNearestGeneratedStructureSpread(
        structureHoldersSet: Set<Holder<Structure>>,
        level: LevelReader,
        structureManager: StructureManager,
        x: Int,
        y: Int,
        z: Int,
        skipKnownStructures: Boolean,
        seed: Long,
        spreadPlacement: RandomSpreadStructurePlacement
    ): JigsawLocateResult? {
        val i = spreadPlacement.spacing()

        for (j in -z..z) {
            val bl = j == -z || j == z

            for (k in -z..z) {
                val bl2 = k == -z || k == z
                if (bl || bl2) {
                    val l = x + i * j
                    val m = y + i * k
                    val chunkPos = spreadPlacement.getPotentialStructureChunk(seed, l, m)
                    val structPos = getMatchingStructureGeneratingAt(
                        structureHoldersSet, level, structureManager, skipKnownStructures, spreadPlacement, chunkPos
                    )
                    if (structPos != null) {
                        return JigsawLocateResult(structPos.first, structPos.second, )
                    }
                }
            }
        }

        return null
    }

    // Same as vanilla code logic-wise, but also checks jigsaw matches
    fun getMatchingStructureGeneratingAt(
        structureHoldersSet: Set<Holder<Structure>>,
        level: LevelReader,
        structureManager: StructureManager,
        skipKnownStructures: Boolean,
        placement: StructurePlacement,
        chunkPos: ChunkPos
    ): Pair<BlockPos, Holder<Structure>>? {
        for (holder in structureHoldersSet) {
            val structureCheckResult = structureManager.checkStructurePresence(chunkPos, holder.value(), placement, skipKnownStructures)
            if (structureCheckResult != StructureCheckResult.START_NOT_PRESENT) {
                val chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS)
                val structureStart = structureManager.getStartForStructure(
                    SectionPos.bottomOf(chunkAccess),
                    holder.value(), chunkAccess
                )
                val containsJigsaw = structureStart?.pieces?.any(::structurePieceMatches)
                // for debug
                /*
                val jigsawIds = structureStart?.pieces?.flatMap { piece ->
                    if (piece is PoolElementStructurePiece) {
                        piece.element.debugConvert()
                    } else {
                        listOf()
                    }
                }
                true // place breakpoint here if needed to debug
                */
                if (
                    structureStart != null && structureStart.isValid && containsJigsaw == true
                    && (!skipKnownStructures || ChunkGenerator.tryAddReference(structureManager, structureStart))
                ) {
                    return Pair.of(placement.getLocatePos(structureStart.chunkPos), holder)
                }
            }
        }

        return null
    }

    private fun StructurePoolElement.debugConvert() : List<ResourceLocation> {
        return when (this) {
            // won't work with runtime elements (aka saved without ids)
            is SinglePoolElement -> listOfNotNull(this.template.left().getOrNull())
            is ListPoolElement -> this.elements.flatMap { it.debugConvert() }
            else -> listOf()
        }
    }

    private fun structurePieceMatches(structurePiece: StructurePiece): Boolean {
        if (structurePiece is PoolElementStructurePiece) {
            return structurePiece.element.matches()
        }
        return false
    }

    private fun StructurePoolElement.matches(): Boolean {
        return when (this) {
            // won't work with runtime elements (aka saved without ids)
            is SinglePoolElement -> this.template.left().map{ targetPieceIds.contains(it) }.orElse(false)
            is ListPoolElement -> this.elements.any { it.matches() }
            else -> false
        }
    }

    private fun shouldStop(): Boolean {
        return isCancelled.value
    }

    private fun onCancel() {
        val reason = cancelReason.value
        RuinsOfGrowsseth.LOGGER.error("Stopped async locate for jigsaw early from $fromPos to ${targetString()} " +
                (if (reason != null) "reason: $reason, " else "") +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        future.complete(null)
    }

    private fun onFinish(result: JigsawLocateResult?) {
        val time = finalTimeMs.updateAndGet { timeElapsedMs() }!!
        RuinsOfGrowsseth.LOGGER.info("Finish async locate for jigsaw early from $fromPos to ${targetString()} in ${time / 1000.0}s" +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        if (result != null) {
            RuinsOfGrowsseth.LOGGER.info("Found $result (took $time ms)")
        } else {
            RuinsOfGrowsseth.LOGGER.info("Not found ${targetString()} (took $time ms)")
        }
        future.complete(result)
    }

    private fun targetString(): String = "$targetPieceIds in [${targetSet.stream().toList().joinToString(", ")}]"

    enum class Phase(val int: Int) {
        CONCENTRIC_RINGS(0),
        RANDOM_SPREAD(1),
    }
}