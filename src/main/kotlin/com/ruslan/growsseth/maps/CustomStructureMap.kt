package com.ruslan.growsseth.maps

import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.filloax.fxlib.*
import com.ruslan.growsseth.mixin.item.mapitem.MapItemAccessor
import com.ruslan.growsseth.utils.AsyncLocator
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.levelgen.structure.BuiltinStructures
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import org.apache.logging.log4j.Level
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture


// Source: https://github.com/TelepathicGrunt/RepurposedStructures/blob/1.19.4-Arch/common/src/main/java/com/telepathicgrunt/repurposedstructures/misc/maptrades/MerchantMapUpdating.java
// adapted to Kotlin plus some changes
/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific position.
 * @param mapStack ItemStack Item stack containing an `Items.FILLED_MAP`
 * @param level ServerLevel
 * @param pos BlockPos target position of the map
 * @param scale Int = 1 Map scale
 * @param destinationType MapDecoration.Type = MapDecoration.Type.TARGET_X Icon to use for the target
 * @param displayName String = null Optional display name to set for the map. "reset" to remove custom names.
 */
fun updateMapToPos(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos,
    scale: Int = 1,
    destinationType: DestinationType = DestinationType.vanilla(MapDecoration.Type.TARGET_X),
    displayName: String? = null,
    unlimitedTracking: Boolean = true,
) {
    MapItemAccessor.callCreateAndStoreSavedData(
        mapStack, level, pos.x, pos.z, scale, true, unlimitedTracking, level.dimension()
    )
    MapItem.renderBiomePreviewMap(level, mapStack)

    if (destinationType.isVanilla) MapItemSavedData.addTargetDecoration(mapStack, pos, "+", destinationType.vanillaType!!)
    else if (destinationType.isCustom) CustomMapData.addTargetCustomDecoration(mapStack, pos, "+", destinationType.customType!!)

    displayName?.let {
        if (it == "reset") {
            mapStack.resetHoverName()
        } else {
            mapStack.hoverName = Component.translatable(it)
        }
    }

    RuinsOfGrowsseth.LOGGER.info("Set map target to: $pos, with icon: $destinationType, name: $displayName (item is $mapStack)")
}

fun invalidateMap(mapStack: ItemStack) {
    mapStack.resetHoverName()
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param mapStack ItemStack Item stack containing an `Items.FILLED_MAP`
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
fun updateMapToStruct(
    mapStack: ItemStack, level: ServerLevel,
    destinationName: String, searchFromPos: BlockPos,
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
    val structTagKey = getStructTagOrKey(destinationName)
    return structTagKey.map({
        updateMapToStruct(mapStack, level, it, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
    }, {
        updateMapToStruct(mapStack, level, it, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
    })
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param mapStack ItemStack Item stack containing an `Items.FILLED_MAP`
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
fun updateMapToStruct(
    mapStack: ItemStack, level: ServerLevel,
    destination: ResourceKey<Structure>, searchFromPos: BlockPos,
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
    return updateMapToStructWithHolder(mapStack, level, getHolderSet(level, destination), searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
}

/**
 * Make an ItemStack containing a `Items.FILLED_MAP` point to a specific structure type in the world.
 * @param mapStack ItemStack Item stack containing an `Items.FILLED_MAP`
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
fun updateMapToStruct(
    mapStack: ItemStack, level: ServerLevel,
    destinationTag: TagKey<Structure>, searchFromPos: BlockPos,
    searchRadius: Int = 50, skipExploredChunks: Boolean = true,
    scale: Int = 1,
    destinationType: DestinationType = DEFAULT_DESTINATION_TYPE,
    displayName: String? = null,
    async: Boolean = false,
): CompletableFuture<Pair<BlockPos, Holder<Structure>>> {
    val holderSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(destinationTag).orElseThrow()
    return updateMapToStructWithHolder(mapStack, level, holderSet, searchFromPos, searchRadius, skipExploredChunks, scale, destinationType, displayName, async)
}

private fun updateMapToStructWithHolder(
    mapStack: ItemStack, level: ServerLevel,
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
        RuinsOfGrowsseth.LOGGER.log(Level.INFO, "Starting async structure '$destString' search...")
        mapStack.hoverName = Component.translatable("menu.working")

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

                updateMapToPos(mapStack, level, pos, scale, finalDestType, displayName ?: "reset")
                RuinsOfGrowsseth.LOGGER.log(Level.INFO, "(async) Found '$destString' at $pos")
            } else {
                invalidateMap(mapStack)
                RuinsOfGrowsseth.LOGGER.log(Level.INFO, "(async) '$destString' not found!")
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
            updateMapToPos(mapStack, level, pos, scale, finalDestType, displayName)
            RuinsOfGrowsseth.LOGGER.log(Level.INFO, "Found '$destString' at $pos")
        } else {
            invalidateMap(mapStack)
            RuinsOfGrowsseth.LOGGER.log(Level.INFO, "'$destString' not found!")
        }
        future.complete(found)
    }

    return future
}

class DestinationType private constructor(val vanillaType: MapDecoration.Type? = null, val customType: CustomMapDecorationType? = null, val auto: Boolean = false) {
    init {
        assert((vanillaType == null) or (customType == null)) { "must set one between customType or vanillaType" }
    }

    val isVanilla get() = vanillaType != null
    val isCustom get() = customType != null

    companion object {
        val EMPTY = DestinationType()
        val AUTO = DestinationType(auto = true)

        fun vanilla(type: MapDecoration.Type): DestinationType {
            return DestinationType(vanillaType = type)
        }
        fun custom(type: CustomMapDecorationType): DestinationType {
            return DestinationType(customType = type)
        }
        fun auto(struct: Holder<Structure>): DestinationType {
            return auto(struct.unwrapKey().get())
        }
        fun auto(structKey: ResourceKey<Structure>): DestinationType {
            VANILLA_STRUCT_ICONS[structKey]?.let { type ->
                return vanilla(type)
            }
            return CustomMapData.decorationTypeByStructure(structKey)?.let { custom(it) } ?: vanilla(MapDecoration.Type.RED_X)
        }
        fun auto(structTag: TagKey<Structure>, registryAccess: RegistryAccess): DestinationType {
            val matchingCustomDecorations = CustomMapData.decorationTypesByStructureTag(registryAccess, structTag)
            if (matchingCustomDecorations.isNotEmpty()) {
                return custom(matchingCustomDecorations.first())
            }
            val tagHolders = registryAccess.registryOrThrow(Registries.STRUCTURE).getTag(structTag).orElseThrow()
            VANILLA_STRUCT_ICONS.forEach {
                tagHolders.forEach { holder ->
                    if (holder.unwrapKey().get().location() == it.key.location()) {
                        return vanilla(it.value)
                    }
                }
            }
            return vanilla(MapDecoration.Type.RED_X)
        }
    }
}

// Private stuff

private val DEFAULT_DESTINATION_TYPE = DestinationType.AUTO

private fun getHolderSet(level: ServerLevel, destination: ResourceKey<Structure>): HolderSet<Structure> {
    val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
    return HolderSet.direct(registry.getHolderOrThrow(destination))
}

private val VANILLA_STRUCT_ICONS = mapOf<ResourceKey<Structure>, MapDecoration.Type>(
    BuiltinStructures.WOODLAND_MANSION to MapDecoration.Type.MANSION,
    BuiltinStructures.OCEAN_MONUMENT to MapDecoration.Type.MONUMENT,
)