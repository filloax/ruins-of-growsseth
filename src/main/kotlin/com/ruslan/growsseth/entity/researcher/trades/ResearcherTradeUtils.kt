package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.FxLibServices
import com.filloax.fxlib.getStructTagOrKey
import com.filloax.fxlib.getYAtXZ
import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.maps.DestinationType
import com.ruslan.growsseth.maps.updateMapToPos
import com.ruslan.growsseth.maps.updateMapToStruct
import com.ruslan.growsseth.mixin.item.mapitem.MapItemAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.saveddata.maps.MapId
import kotlin.jvm.optionals.getOrNull

object ResearcherTradeUtils {
    private val fixedStructureGeneration = FxLibServices.fixedStructureGeneration

    private fun getServer(researcher: Researcher) = researcher.server ?: throw IllegalStateException("Cannot access TradeProvider from client!")

    fun getMatchingStructures(registryAccess: RegistryAccess, tagOrId: String): List<ResourceLocation> {
        val tagOrKey = getStructTagOrKey(tagOrId)
        return tagOrKey.map({ tag ->
            registryAccess
                .registryOrThrow(Registries.STRUCTURE).getOrCreateTag(tag)
                .mapNotNull { h -> h.unwrapKey().map { it.location() }.getOrNull() }
        }, { key ->
            listOf(key.location())
        })
    }

    // Should be ran once per item stack
    fun setTradeMapTarget(researcher: Researcher, itemStack: ItemStack, mapData: TradeItemMapInfo, offer: MerchantOffer) {
        val server = getServer(researcher)
        val level = researcher.level() as ServerLevel
        val scale = mapData.scale ?: 3
        var known = false

        synchronized(researcher.storedMapLocations) {
            researcher.storedMapLocations[mapData.structure]?.let { mapMemory ->
                val destinationType = mapMemory.struct.map({
                    DestinationType.auto(it, server.registryAccess())
                }, {
                    DestinationType.auto(it)
                })
                updateMapToPos(
                    itemStack,
                    level,
                    mapMemory.pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                val mapSavedData = MapItem.getSavedData(MapId(mapMemory.mapId), researcher.level())
                if (mapSavedData != null) {
                    MapItemAccessor.callStoreMapData(itemStack, mapMemory.mapId)
                } else {
                    RuinsOfGrowsseth.LOGGER.info(
                        "Tried setting res. known map data from id ${mapMemory.mapId} but was null, making new"
                    )
                }
                RuinsOfGrowsseth.LOGGER.info("Loaded map data from known map $mapMemory")
                known = true
            }
        }

        if (!known) {
            var pos: BlockPos? = null
            if (mapData.x != null && mapData.z != null) {
                pos = BlockPos(mapData.x, getYAtXZ(level, mapData.x, mapData.z), mapData.z)
            } else if (mapData.fixedStructureId != null) {
                val matchingStructures = getMatchingStructures(level.registryAccess(), mapData.fixedStructureId)
                val spawnData = fixedStructureGeneration.registeredStructureSpawns.values
                    .filter { matchingStructures.contains(it.structure) }
                    .minByOrNull { it.pos.distManhattan(researcher.blockPosition()) }
                if (spawnData != null) {
                    pos = spawnData.pos
                } else {
                    RuinsOfGrowsseth.LOGGER.warn("Map $mapData has structure id not found in fixed spawns: ${mapData.fixedStructureId}")
                }
            }
            if (pos != null) {
                val destination = getStructTagOrKey(mapData.structure)
                val destinationType = destination.map({
                    DestinationType.auto(it, server.registryAccess())
                }, {
                    DestinationType.auto(it)
                })

                updateMapToPos(
                    itemStack,
                    level,
                    pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                RuinsOfGrowsseth.LOGGER.info("Res.trades: created map to pos $pos dtype $destinationType")

                synchronized(researcher.storedMapLocations) {
                    researcher.storedMapLocations[mapData.structure] = Researcher.MapMemory(
                        pos,
                        destination,
                        itemStack[DataComponents.MAP_ID]?.id ?: throw IllegalStateException("Map has no id after updating pos! $itemStack"),
                    )
                }
                researcher.refreshCurrentTrades()
                known = true
            }
        }

        if (!known) {
            // For community version (locate map functionality)
            updateMapToStruct(
                itemStack,
                level,
                mapData.structure,
                researcher.blockPosition(),
                scale,
                displayName = mapData.name,
                async = true,
            ).thenAccept {
                val pos = it.first
                if (pos != null) {
                    RuinsOfGrowsseth.LOGGER.info("Res.trades: found map to pos $pos struct ${mapData.structure}")
                    synchronized(researcher.storedMapLocations) {
                        researcher.storedMapLocations[mapData.structure] = Researcher.MapMemory(
                            pos,
                            Either.right(it.second.unwrapKey().get()),
                            itemStack[DataComponents.MAP_ID]?.id ?: throw IllegalStateException("Map item has no id after updating! $itemStack"),
                        )
                    }
                    researcher.refreshCurrentTrades()
                } else {
                    offer.increaseUses() // Disable offer, as it's always generated with 1 use
                }
            }
        }
    }
}