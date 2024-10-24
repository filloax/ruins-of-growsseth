package com.ruslan.growsseth.structure.locate

import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.locate.LocateTask.Phase
import com.ruslan.growsseth.utils.matchesJigsaw
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import it.unimi.dsi.fastutil.objects.ObjectArraySet
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
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

typealias StructLocatePredicate = (StructureStart, ChunkPos) -> Boolean
typealias PositionAdjustFunction = (LocateResult, StructureStart) -> LocateResult?

// Adapted from AsyncLocator, move to kotlin and allow stopping
// Original: https://github.com/thebrightspark/AsyncLocator/blob/1.19.x/src/main/java/brightspark/asynclocator/AsyncLocator.java
object StoppableAsyncLocator {
    private var executorService: ExecutorService? = null

    private fun setupExecutorService() {
        shutdownExecutorService()

        val threads = 1
        RuinsOfGrowsseth.LOGGER.info("Starting locating executor service with {} threads", threads)
        executorService = Executors.newFixedThreadPool(
            threads,
            object : ThreadFactory {
                private val poolNum = AtomicInteger(1)
                private val threadNum = AtomicInteger(1)
                private val namePrefix = "growsseth-" + poolNum.getAndIncrement() + "-locator-thread-"

                override fun newThread(r: Runnable): Thread {
                    return Thread(null, r, namePrefix + threadNum.getAndIncrement())
                }
            }
        )
    }

    private fun shutdownExecutorService() {
        RuinsOfGrowsseth.LOGGER.info("Stopping locating executor service")
        executorService?.shutdown()
    }

    /**
     * Queues a task to locate a feature and returns a
     * [LocateTask] that allows monitoring, cancelling it, and running code with futures.
     *
     * The targetFilter and positionAdjustment params only work with RandomSpread placements.
     *
     * @param targetFilter A predicate to filter only matching structures
     * @param positionAdjustment Adjust the final result position depending on the StructureStart
     */
    fun locate(
        level: ServerLevel, structureSet: HolderSet<Structure>, pos: BlockPos,
        searchRadius: Int, skipKnownStructures: Boolean,
        targetFilter: StructLocatePredicate? = null,
        positionAdjustment: PositionAdjustFunction? = null,
        timeoutSeconds: Int? = null,
        signalProgress: SignalProgressFun? = null,
    ): LocateTask {
        val task = LocateTask(
            level.chunkSource.generator, level, pos,
            structureSet, targetFilter, positionAdjustment,
            searchRadius, skipKnownStructures, signalProgress,
        )
        val timeoutThread = timeoutSeconds?.let { timeout -> thread(start = false) {
            Thread.sleep(timeout * 1000L)
            if (!task.done)
                task.cancel("timeout")
        } }
        executorService?.submit {
            timeoutThread?.start()
            task.start()
        } ?: throw IllegalStateException("No executorservice for locate!")
        return task
    }

    /**
     * Queues a task to locate a structure with a specific jigsaw piece inside it and returns a
     * [JigsawLocateTask] that allows monitoring, cancelling it, and running code with futures.
     */
    fun locateJigsaw(
        level: ServerLevel, structureSet: HolderSet<Structure>,
        jigsawIds: Collection<ResourceLocation>,
        pos: BlockPos,
        searchRadius: Int, skipKnownStructures: Boolean,
        timeoutSeconds: Int? = null,
        signalProgress: SignalProgressFun? = null,
    ): LocateTask {
        return locate(
            level, structureSet, pos,
            searchRadius, skipKnownStructures,
            { structureStart, _ -> structureStart.pieces.any { piece ->
                piece.matchesJigsaw(jigsawIds)
            } },
            { locateResult, structureStart ->
                structureStart.pieces.first { piece ->
                    piece.matchesJigsaw(jigsawIds)
                }?.let { LocateResult(it.locatorPosition, locateResult.structure, structureStart) }
            },
            timeoutSeconds, signalProgress
        )
    }

    object Callbacks {
        fun onServerStarting() {
            setupExecutorService()
        }

        fun onServerStopping() {
            shutdownExecutorService()
        }
    }
}

typealias SignalProgressFun = (LocateTask, Phase, Float) -> Unit

/**
 * Code analogous to vanilla code, but unlike
 * AsyncLocator can be interrupted if it takes too long (or for any other reason).
 * Side effect is that it won't be affected by locate mixins
 */
class LocateTask(
    private val chunkGenerator: ChunkGenerator,
    val level: ServerLevel,
    val fromPos: BlockPos,
    val targetSet: HolderSet<Structure>,
    val targetFilter: StructLocatePredicate?,
    val positionAdjustment: PositionAdjustFunction?,
    val searchRadius: Int,
    val skipKnownStructures: Boolean,
    private val signalProgress: SignalProgressFun?
) {
    private val server = level.server
    private lateinit var startTime: Instant
    private val future = CompletableFuture<LocateResult>()
    var done = false
        private set
    private var isCancelled = false
    private var cancelReason: String? = null
    private var finalTimeMs: Long? = null

    private val cancelLock = ReentrantLock()

    fun start() {
        try {
            startTime = Clock.System.now()

            RuinsOfGrowsseth.LOGGER.info(
                "[chunkGenerator] Trying to locate {} in {} around {} within {} chunks",
                targetString(), level, fromPos, searchRadius
            )

            val result = findNearestMapStructure()
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
            done = true
        }
    }

    fun then(action: (LocateResult?) -> Unit): LocateTask {
        future.thenAccept(action)
        return this
    }

    fun thenOnServerThread(action: (LocateResult?) -> Unit): LocateTask {
        future.thenAccept{ result -> server.submit{ simpleTryCatch{ action(result) } } }
        return this
    }

    fun onException(action: (e: Throwable) -> LocateResult?) {
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
        RuinsOfGrowsseth.LOGGER.warn("Stopping async locate for structure ${targetString()} from pos $fromPos after ${timeElapsedMs() / 1000.0}s, " +
                (if (reason != null) "reason: $reason, " else "") +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        cancelLock.apply {
            cancelReason = reason
            isCancelled = true
        }
    }

    /**
     * Code analoguous to base [ChunkGenerator.findNearestMapStructure],
     * but with added checks and signals
     */
    private fun findNearestMapStructure(): Pair<Boolean, LocateResult> {
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
            var output: LocateResult? = null
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
                if (targetFilter != null) {
                    RuinsOfGrowsseth.LOGGER.warn("Filter (eg jigsaw) for concentric rings NYI, but only needed for villages (random spread) right now")
                }

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
                            output = LocateResult(ringsResult.first, ringsResult.second)
                        }
                    }

                    signalProgress?.let { it(this, Phase.CONCENTRIC_RINGS, (i.toFloat() + 1) / structuresSize) }
                }
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
                        val spreadResult = getMatchingNearestGeneratedStructureSpread(
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
                        return Pair(true, finalizeResult(output!!))
                    }
                }
            }

            return Pair(true, finalizeResult(output!!))
        }
    }

    fun finalizeResult(result: LocateResult): LocateResult {
        if (result.structureStart == null) return result

        return positionAdjustment?.let { it(result, result.structureStart) } ?: result
    }

    // Same as vanilla code, but can call filter function
    private fun getMatchingNearestGeneratedStructureSpread(
        structureHoldersSet: Set<Holder<Structure>>,
        level: LevelReader,
        structureManager: StructureManager,
        x: Int,
        y: Int,
        z: Int,
        skipKnownStructures: Boolean,
        seed: Long,
        spreadPlacement: RandomSpreadStructurePlacement
    ): LocateResult? {
        val i = spreadPlacement.spacing()

        for (j in -z..z) {
            val bl = j == -z || j == z

            for (k in -z..z) {
                val bl2 = k == -z || k == z
                if (bl || bl2) {
                    val l = x + i * j
                    val m = y + i * k
                    val chunkPos = spreadPlacement.getPotentialStructureChunk(seed, l, m)
                    val result = getMatchingStructureGeneratingAt(
                        structureHoldersSet, level, structureManager, skipKnownStructures, spreadPlacement, chunkPos
                    )
                    if (result != null) {
                        return result
                    }
                }
            }
        }

        return null
    }

    // Same as vanilla code logic-wise, but also checks predicates (example: jigsaw matches)
    fun getMatchingStructureGeneratingAt(
        structureHoldersSet: Set<Holder<Structure>>,
        level: LevelReader,
        structureManager: StructureManager,
        skipKnownStructures: Boolean,
        placement: StructurePlacement,
        chunkPos: ChunkPos
    ): LocateResult? {
        for (holder in structureHoldersSet) {
            val structureCheckResult = structureManager.checkStructurePresence(chunkPos, holder.value(), placement, skipKnownStructures)
            if (structureCheckResult != StructureCheckResult.START_NOT_PRESENT) {
                // TODO: vanilla code does this, not doing this might be causing the server crash? investigate
//                if (!skipKnownStructures && structureCheckResult == StructureCheckResult.START_PRESENT) {
//                    return Pair.of(placement.getLocatePos(chunkPos), holder)
//                }

                val chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS)
                val structureStart = structureManager.getStartForStructure(
                    SectionPos.bottomOf(chunkAccess),
                    holder.value(), chunkAccess
                )
                if (
                    structureStart != null && structureStart.isValid && targetFilter?.let { it(structureStart, chunkPos) } != false
                    && (!skipKnownStructures || ChunkGenerator.tryAddReference(structureManager, structureStart))
                ) {
                    return LocateResult(placement.getLocatePos(structureStart.chunkPos), holder, structureStart)
                }
            }
        }

        return null
    }

    private fun shouldStop(): Boolean {
        return cancelLock.let { isCancelled }
    }

    private fun onCancel() {
        val reason = cancelLock.let { cancelReason }
        RuinsOfGrowsseth.LOGGER.error("Stopped async locate early from $fromPos to ${targetString()} " +
                (if (reason != null) "reason: $reason, " else "") +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        future.complete(null)
    }

    private fun onFinish(result: LocateResult?) {
        val time = timeElapsedMs()
        finalTimeMs = time
        RuinsOfGrowsseth.LOGGER.info("Finish async locate early from $fromPos to ${targetString()} in ${time / 1000.0}s" +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        if (result != null) {
            RuinsOfGrowsseth.LOGGER.info("Found $result (took $time ms)")
        } else {
            RuinsOfGrowsseth.LOGGER.info("Not found ${targetString()} (took $time ms)")
        }
        future.complete(result)
    }

    private fun targetString(): String = "[${targetSet.stream().toList().joinToString(", ")}, filtered=${targetFilter!=null}]"

    enum class Phase(val int: Int) {
        CONCENTRIC_RINGS(0),
        RANDOM_SPREAD(1),
    }
}

data class LocateResult(val pos: BlockPos, val structure: Holder<Structure>, val structureStart: StructureStart? = null)