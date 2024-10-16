package com.ruslan.growsseth.maps

import com.filloax.fxlib.api.FxLibServices
import com.filloax.fxlib.api.getStructTagOrKey
import com.filloax.fxlib.api.loreLines
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.structure.locate.LocateResult
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
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.MapDecorations
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.random.Random

fun ItemStack.createAndStoreMapData(
    level: Level, x: Int, z: Int, scale: Int,
    trackingPosition: Boolean, unlimitedTracking: Boolean
) {
    val newMapItem = MapItem.create(level, x, z, scale.toByte(), trackingPosition, unlimitedTracking)
    this[DataComponents.MAP_ID] = newMapItem[DataComponents.MAP_ID]
}

private val NON_TARGET_DECO_TYPES = setOf<Holder<MapDecorationType>>(
    MapDecorationTypes.PLAYER,
    MapDecorationTypes.FRAME,
    MapDecorationTypes.PLAYER_OFF_MAP,
    MapDecorationTypes.PLAYER_OFF_LIMITS,
    MapDecorationTypes.WHITE_BANNER,
    MapDecorationTypes.ORANGE_BANNER,
    MapDecorationTypes.MAGENTA_BANNER,
    MapDecorationTypes.LIGHT_BLUE_BANNER,
    MapDecorationTypes.YELLOW_BANNER,
    MapDecorationTypes.LIME_BANNER,
    MapDecorationTypes.PINK_BANNER,
    MapDecorationTypes.GRAY_BANNER,
    MapDecorationTypes.LIGHT_GRAY_BANNER,
    MapDecorationTypes.CYAN_BANNER,
    MapDecorationTypes.PURPLE_BANNER,
    MapDecorationTypes.BLUE_BANNER,
    MapDecorationTypes.BROWN_BANNER,
    MapDecorationTypes.GREEN_BANNER,
    MapDecorationTypes.RED_BANNER,
    MapDecorationTypes.BLACK_BANNER,
)

fun ItemStack.getMapTargetIcon(): DestinationType? {
    return this[DataComponents.MAP_DECORATIONS]?.let { decorations ->
        decorations.decorations.values.firstOrNull<MapDecorations.Entry> {
            !NON_TARGET_DECO_TYPES.contains(it.type)
        }?.let { DestinationType.withIcon(it.type) }
    }
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
    destinationType: DestinationType = DestinationType.withIcon(MapDecorationTypes.TARGET_X),
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
 * Parameters for the update map functions, used to simplify adding new parameters to
 * all the public functions
 * @param searchFromPos BlockPos Position to start the structure search from.
 * @param searchRadius Int = 50 Radius of the search.
 * @param skipExploredChunks Bool = true Set to true to ignore chunks that were already generated.
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map
 * @param mustContainJigsawIds If set, will only locate structures that contain jigsaw pieces with ids contained in this Set
 */
data class MapLocateContext(
    val searchFromPos: BlockPos,
    val searchRadius: Int = 100,
    val skipExploredChunks: Boolean = false,
    val scale: Int = 1,
    val overrideDestinationType: DestinationType? = null,
    val displayName: String? = null,
    val mustContainJigsawIds: Collection<ResourceLocation>? = null,
    val searchTimeoutSeconds: Int = 15,
)

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destinationName String Target structure id or tag to search for and target.
 * @param context See [MapLocateContext]
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destinationName: String,
    context: MapLocateContext,
): CompletableFuture<LocateResult> {
    val structTagKey = getStructTagOrKey(destinationName)
    return structTagKey.map({
        updateMapToStruct(level, it, context)
    }, {
        updateMapToStruct(level, it, context)
    })
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destination ResourceKey<Structure> Target structure id to search for and target.
 * @param context See [MapLocateContext]
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destination: ResourceKey<Structure>,
    context: MapLocateContext,
): CompletableFuture<LocateResult> {
    return updateMapToStructWithHolder(level, getHolderSet(level, destination), context)
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param level ServerLevel
 * @param destinationTag TagKey<Structure> Target structure tag to search for and target.
 * @param context See [MapLocateContext]
 */
fun ItemStack.updateMapToStruct(
    level: ServerLevel,
    destinationTag: TagKey<Structure>,
    context: MapLocateContext,
): CompletableFuture<LocateResult> {
    val holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(destinationTag).orElseThrow()
    return updateMapToStructWithHolder(level, holderSet, context)
}

// Private stuff

private fun ItemStack.updateMapToStructWithHolder(
    level: ServerLevel,
    destinationHolderSet: HolderSet<Structure>,
    context: MapLocateContext
): CompletableFuture<LocateResult> {
    val doSkipExploredChunks = context.skipExploredChunks
    // In general, return CompletableFuture *separately* from locatetask
    // to make sure things added to it as a return of this run after the locatetask's
    // callback here
    val future = CompletableFuture<LocateResult>()

    // Inner class to make use of atomic (can only be used in initializers of classes)
    // yes, this is ugly, might improve later (why did it work in the fabric-only module?
    class UpdateMapTask {
        private val done = atomic<Boolean>(false)

        fun run() {
            val destString = destinationHolderSet.toString()
            RuinsOfGrowsseth.LOGGER.info("Starting async structure '$destString' search...")

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

            val task = if (context.mustContainJigsawIds == null)
                StoppableAsyncLocator.locate(
                    level,
                    destinationHolderSet,
                    context.searchFromPos,
                    context.searchRadius,
                    doSkipExploredChunks,
                    timeoutSeconds = context.searchTimeoutSeconds,
                    signalProgress = signalProgress,
                )
            else
                StoppableAsyncLocator.locateJigsaw(
                    level,
                    destinationHolderSet,
                    context.mustContainJigsawIds,
                    context.searchFromPos,
                    context.searchRadius,
                    doSkipExploredChunks,
                    timeoutSeconds = context.searchTimeoutSeconds,
                    signalProgress = signalProgress,
                )
            task.thenOnServerThread { result ->
                done.update { true }
                if (result != null) {
                    val pos = result.pos
                    val finalDestType = context.overrideDestinationType ?: DestinationType.auto(result.structure)

                    updateMapToPos(level, pos, context.scale, finalDestType, context.displayName ?: "reset")
                    RuinsOfGrowsseth.LOGGER.info("(async) Found '$destString' at $pos")
                } else {
                    invalidateMap()
                    RuinsOfGrowsseth.LOGGER.info("(async) '$destString' not found!")
                }
                future.complete(result)
            }
        }
    }

    this.setLoadingName(context.displayName)
    UpdateMapTask().run()

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

private fun getHolderSet(level: ServerLevel, destination: ResourceKey<Structure>): HolderSet<Structure> {
    val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
    return HolderSet.direct(registry.getHolderOrThrow(destination))
}