package com.ruslan.growsseth.structure.locate

import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.locate.LocateTask.Phase
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
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement
import java.text.NumberFormat
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

typealias LocateResult = Pair<BlockPos, Holder<Structure>>?

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
     * Queues a task to locate a feature using
     * [ChunkGenerator.findNearestMapStructure] and returns a
     * [LocateTask] that allows monitoring, cancelling it, and running code with futures.
     */
    fun locate(
        level: ServerLevel, structureSet: HolderSet<Structure>, pos: BlockPos,
        searchRadius: Int, skipKnownStructures: Boolean,
        timeoutSeconds: Int? = null,
        signalProgress: SignalProgressFun? = null,
    ): LocateTask {
        val task = LocateTask(
            level.chunkSource.generator, level, pos,
            structureSet, searchRadius, skipKnownStructures,
            signalProgress
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
    val searchRadius: Int,
    val skipKnownStructures: Boolean,
    private val signalProgress: SignalProgressFun?
) {
    private val server = level.server
    private lateinit var startTime: Instant
    private val isCancelled = atomic<Boolean>(false)
    private val cancelReason = atomic<String?>(null)
    private val finalTimeMs = atomic<Long?>(null)
    private val future = CompletableFuture<LocateResult>()
    private val done_ = atomic<Boolean>(false)

    val done get() = done_.value

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
            done_.update { true }
        }
    }

    fun then(action: (LocateResult) -> Unit): LocateTask {
        future.thenAccept(action)
        return this
    }

    fun thenOnServerThread(action: (LocateResult) -> Unit): LocateTask {
        future.thenAccept{ result -> server.submit{ simpleTryCatch{ action(result) } } }
        return this
    }

    fun onException(action: (e: Throwable) -> Pair<BlockPos, Holder<Structure>>?) {
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
        cancelReason.update { reason }
        isCancelled.update { true }
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
            var output: Pair<BlockPos, Holder<Structure>>? = null
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
                        val spreadResult = ChunkGenerator.getNearestGeneratedStructure(
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
                            val dist = fromPos.distSqr(spreadResult.first)
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

    private fun shouldStop(): Boolean {
        return isCancelled.value
    }

    private fun onCancel() {
        val reason = cancelReason.value
        RuinsOfGrowsseth.LOGGER.error("Stopped async locate early from $fromPos to ${targetString()} " +
                (if (reason != null) "reason: $reason, " else "") +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        future.complete(null)
    }

    private fun onFinish(result: Pair<BlockPos, Holder<Structure>>?) {
        val time = finalTimeMs.updateAndGet { timeElapsedMs() }!!
        RuinsOfGrowsseth.LOGGER.info("Finish async locate early from $fromPos to ${targetString()} in ${time / 1000.0}s" +
                "params were searchRadius=$searchRadius skipKnownStructures=$skipKnownStructures")
        if (result != null) {
            RuinsOfGrowsseth.LOGGER.info("Found $result (took $time ms)")
        } else {
            RuinsOfGrowsseth.LOGGER.info("Not found ${targetString()} (took $time ms)")
        }
        future.complete(result)
    }

    private fun targetString(): String = "[${targetSet.stream().toList().joinToString(", ")}]"

    enum class Phase(val int: Int) {
        CONCENTRIC_RINGS(0),
        RANDOM_SPREAD(1),
    }
}