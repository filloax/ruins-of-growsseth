package com.ruslan.growsseth.maps

import com.filloax.fxlib.*
import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.utils.AsyncLocator
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
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.util.concurrent.CompletableFuture

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

    RuinsOfGrowsseth.LOGGER.info("Set map target to: $pos, with icon: $destinationType, name: $displayName (item is $this)")
}

fun ItemStack.invalidateMap() {
    this.remove(DataComponents.CUSTOM_NAME)
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
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
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
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
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
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
    val holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(destinationTag).orElseThrow()
    return updateMapToStructWithHolder(level, holderSet, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
}

private fun ItemStack.updateMapToStructWithHolder(
    level: ServerLevel,
    destinationHolderSet: HolderSet<Structure>, searchFromPos: BlockPos,
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
    // In general, return CompletableFuture *separately* from locatetask
    // to make sure things added to it as a return of this run after the locatetask's
    // callback here
    val future = CompletableFuture<Pair<BlockPos, Holder<Structure>>>()
    val destString = destinationHolderSet.unwrapKey().toString()
    if (async) {
        RuinsOfGrowsseth.LOGGER.info("Starting async structure '$destString' search...")
        this[DataComponents.CUSTOM_NAME] = Component.translatable("menu.working")

        AsyncLocator.locate(
            level,
            destinationHolderSet,
            searchFromPos,
            searchRadius,
            skipExploredChunks
        ).thenOnServerThread {
            val pos = it.first
            if (pos != null) {
                val finalDestType = if (destinationType.auto) DestinationType.auto(it.second) else destinationType

                updateMapToPos(level, pos, scale, finalDestType, displayName ?: "reset")
                RuinsOfGrowsseth.LOGGER.info("(async) Found '$destString' at $pos")
            } else {
                invalidateMap()
                RuinsOfGrowsseth.LOGGER.info("(async) '$destString' not found!")
            }
            future.complete(it)
        }
    } else {
        val found = level.chunkSource.generator
            .findNearestMapStructure(level, destinationHolderSet, searchFromPos, searchRadius, skipExploredChunks)
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



// Private stuff

private val DEFAULT_DESTINATION_TYPE = DestinationType.AUTO

private fun getHolderSet(level: ServerLevel, destination: ResourceKey<Structure>): HolderSet<Structure> {
    val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
    return HolderSet.direct(registry.getHolderOrThrow(destination))
}