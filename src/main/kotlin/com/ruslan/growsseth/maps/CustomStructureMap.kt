package com.ruslan.growsseth.maps

import com.filloax.fxlib.*
import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.locate.LocateResult
import com.ruslan.growsseth.structure.locate.LocateTask
import com.ruslan.growsseth.structure.locate.SignalProgressFun
import com.ruslan.growsseth.structure.locate.StoppableAsyncLocator
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.datetime.Clock
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.util.concurrent.CompletableFuture
import javax.xml.crypto.Data
import kotlin.concurrent.thread
import kotlin.math.sign
import kotlin.random.Random

private const val DEFAULT_ASYNC = true
// true seems to be way slower even in single threaded mode
private const val DEFAULT_SKIP_EXPLORED = false
private const val DEFAULT_SEARCH_RANGE = 100
private const val DEFAULT_SEARCH_TIMEOUT_S = 10

fun ItemStack.createAndStoreMapData(
    level: Level, x: Int, z: Int, scale: Int,
    trackingPosition: Boolean, unlimitedTracking: Boolean
) {
    val newMapItem = MapItem.create(level, x, z, scale.toByte(), trackingPosition, unlimitedTracking)
    this[DataComponents.MAP_ID] = newMapItem[DataComponents.MAP_ID]
}

// Source: https://github.com/TelepathicGrunt/RepurposedStructures/blob/1.19.4-Arch/common/src/main/java/com/telepathicgrunt/repurposedstructures/misc/maptrades/MerchantMapUpdating.java
// adapted to Kotlin plus some changes
/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific position.
 * @param level ServerLevel
 * @param pos BlockPos target position of the map
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map. "reset" to remove custom names.
 */
fun ItemStack.updateMapToPos(
    level: ServerLevel,
    pos: BlockPos,
    scale: Int = 1,
    destinationType: DestinationType = DestinationType.vanilla(MapDecorationTypes.TARGET_X),
    displayName: String? = null,
    unlimitedTracking: Boolean = true,
) {
    createAndStoreMapData(
        level, pos.x, pos.z, scale, true, unlimitedTracking
    )
    MapItem.renderBiomePreviewMap(level, this)

    if (destinationType.isSet) MapItemSavedData.addTargetDecoration(this, pos, "+", destinationType.type!!)

    displayName?.let {
        if (it == "reset") {
            this.remove(DataComponents.CUSTOM_NAME)
        } else {
            this[DataComponents.CUSTOM_NAME] = Component.translatable(it)
        }
    }
    this.loreLines().clear()

    RuinsOfGrowsseth.LOGGER.info("Set map target to: $pos, with icon: $destinationType, name: $displayName (item is $this)")
}

fun ItemStack.invalidateMap() {
    this.setMapFailedName()
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destinationName String Target structure id or tag to search for and target.
 * @param searchFromPos BlockPos Position to start the structure search from.
 * @param searchRadius Int = 50 Radius of the search.
 * @param skipExploredChunks Bool = true Set to true to ignore chunks that were already generated.
 * @param async Bool = false Run the search asynchronously, better for performance. NYI
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destinationName: String, searchFromPos: BlockPos,
    searchRadius: Int = DEFAULT_SEARCH_RANGE, skipExploredChunks: Boolean? = null,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean? = null,
): CompletableFuture<LocateResult> {
    val structTagKey = getStructTagOrKey(destinationName)
    return structTagKey.map({
        updateMapToStruct(level, it, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
    }, {
        updateMapToStruct(level, it, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
    })
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destination ResourceKey<Structure> Target structure id to search for and target.
 * @param searchFromPos BlockPos Position to start the structure search from.
 * @param searchRadius Int = 50 Radius of the search.
 * @param skipExploredChunks Bool = true Set to true to ignore chunks that were already generated.
 * @param async Bool = false Run the search asynchronously, better for performance. NYI
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destination: ResourceKey<Structure>, searchFromPos: BlockPos,
    searchRadius: Int = DEFAULT_SEARCH_RANGE, skipExploredChunks: Boolean? = null,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean? = null,
): CompletableFuture<LocateResult> {
    return updateMapToStructWithHolder(level, getHolderSet(level, destination), searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destinationTag TagKey<Structure> Target structure tag to search for and target.
 * @param searchFromPos BlockPos Position to start the structure search from.
 * @param searchRadius Int = 50 Radius of the search.
 * @param skipExploredChunks Bool = true Set to true to ignore chunks that were already generated.
 * @param async Bool = false Run the search asynchronously, better for performance. NYI
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destinationTag: TagKey<Structure>, searchFromPos: BlockPos,
    searchRadius: Int = DEFAULT_SEARCH_RANGE, skipExploredChunks: Boolean? = null,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean? = null,
): CompletableFuture<LocateResult> {
    val holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(destinationTag).orElseThrow()
    return updateMapToStructWithHolder(level, holderSet, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
}

private fun ItemStack.updateMapToStructWithHolder(
    level: ServerLevel,
    destinationHolderSet: HolderSet<Structure>, searchFromPos: BlockPos,
    searchRadius: Int, skipExploredChunks: Boolean?,
    scale: Int,
    destinationType: DestinationType,
    displayName: String?,
    async: Boolean?,
): CompletableFuture<LocateResult> {
    val doSkipExploredChunks = skipExploredChunks ?: DEFAULT_SKIP_EXPLORED
    // In general, return CompletableFuture *separately* from locatetask
    // to make sure things added to it as a return of this run after the locatetask's
    // callback here
    val future = CompletableFuture<LocateResult>()
    val destString = destinationHolderSet.unwrapKey().toString()
    if (async == true || (async == null && DEFAULT_ASYNC)) {
        RuinsOfGrowsseth.LOGGER.info("Starting async structure '$destString' search...")
        this.setLoadingName(displayName)

        val done = atomic<Boolean>(false)
        val startTime = Clock.System.now()

        thread(name="locator-timing-thread", start = true, isDaemon = true){
            while (!done.value) {
                Thread.sleep(10000)
                if (!done.value) {
                    val time = Clock.System.now()
                    RuinsOfGrowsseth.LOGGER.info("Async structure '$destString' search still running, took ${(time - startTime).inWholeMilliseconds/1000}s")
                }
            }
        }

        val signalProgress: SignalProgressFun? = if (FxLibServices.platform.isDevEnvironment()) { { task, phase, pct ->
            RuinsOfGrowsseth.LOGGER.info("Locate $destString progress: phase %s | %.2f%% | %.2fs".format(phase, pct * 100, task.timeElapsedMs() / 1000.0))
        } } else null

        StoppableAsyncLocator.locate(
            level,
            destinationHolderSet,
            searchFromPos,
            searchRadius,
            doSkipExploredChunks,
            timeoutSeconds = DEFAULT_SEARCH_TIMEOUT_S,
            signalProgress = signalProgress,
        ).thenOnServerThread { result ->
            done.update { true }
            if (result != null) {
                val pos = result.first
                val finalDestType = if (destinationType.auto) DestinationType.auto(result.second) else destinationType

                updateMapToPos(level, pos, scale, finalDestType, displayName ?: "reset")
                RuinsOfGrowsseth.LOGGER.info("(async) Found '$destString' at $pos")
            } else {
                invalidateMap()
                RuinsOfGrowsseth.LOGGER.info("(async) '$destString' not found!")
            }
            future.complete(result)
        }
    } else {
        RuinsOfGrowsseth.LOGGER.info("Starting single-thread structure '$destString' search...")
        val found = level.chunkSource.generator.findNearestMapStructure(level, destinationHolderSet, searchFromPos, 100, false)

        if (found == null) {
            RuinsOfGrowsseth.LOGGER.error("Structure $destinationHolderSet not found!")
            future.completeExceptionally(IllegalStateException("Structure $destinationHolderSet not found!"))
            return future
        }

        val pos = found.first
        if (pos != null) {
            val finalDestType = if (destinationType.auto) DestinationType.auto(found.second) else destinationType
            updateMapToPos(level, pos, scale, finalDestType, displayName)
            RuinsOfGrowsseth.LOGGER.info("Found '$destString' at $pos")
        } else {
            invalidateMap()
            RuinsOfGrowsseth.LOGGER.info("'$destString' not found!")
        }
        future.complete(found)
    }

    return future
}

private val loadingNameRandom = Random(Clock.System.now().toEpochMilliseconds())

private fun ItemStack.setLoadingName(displayName: String?) {
    this[DataComponents.CUSTOM_NAME] = Component.translatable("item.growsseth.map.loadingName")
    val loadingId = loadingNameRandom.nextInt(3) + 1
    this.loreLines().apply {
        clear()
        add(Component.translatable("item.growsseth.map.loading$loadingId"))
        if (displayName != null)
            add(Component.translatable(displayName).withStyle(ChatFormatting.DARK_GRAY))
    }
}

private fun ItemStack.setMapFailedName() {
    this[DataComponents.CUSTOM_NAME] = Component.translatable("item.growsseth.map.loadingFail").withStyle(ChatFormatting.RED)
    this.loreLines().apply {
        clear()
        add(Component.translatable("item.growsseth.map.loadingFailLore").withStyle(ChatFormatting.RED))
    }
    CustomData.update(DataComponents.CUSTOM_DATA, this) { tag -> tag.putBoolean(Constants.ITEM_TAG_MAP_FAILED_LOCATE, true) }
}



// Private stuff

private val DEFAULT_DESTINATION_TYPE = DestinationType.AUTO

private fun getHolderSet(level: ServerLevel, destination: ResourceKey<Structure>): HolderSet<Structure> {
    val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
    return HolderSet.direct(registry.getHolderOrThrow(destination))
}