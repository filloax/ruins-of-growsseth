package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.api.FxLibServices
import com.filloax.fxlib.api.getStructTagOrKey
import com.filloax.fxlib.api.getYAtXZ
import com.filloax.fxlib.api.loreLines
import com.filloax.fxlib.api.nbt.loadField
import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.maps.DestinationType
import com.ruslan.growsseth.maps.MapLocateContext
import com.ruslan.growsseth.maps.updateMapToPos
import com.ruslan.growsseth.maps.updateMapToStruct
import com.ruslan.growsseth.templates.BookTemplates
import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
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
        val registryAccess = server.registryAccess()
        val level = researcher.level() as ServerLevel
        val scale = mapData.scale ?: 3
        var known = false

        synchronized(researcher.storedMapLocations) {
            researcher.storedMapLocations[mapData.structure]?.let { mapMemory ->
                val destinationType = mapData.overrideMapIcon?.let { DestinationType.withIcon(it, registryAccess) } ?: mapMemory.struct.map({
                    DestinationType.auto(it, server.registryAccess())
                }, {
                    DestinationType.auto(it)
                })
                itemStack.updateMapToPos(
                    level,
                    mapMemory.pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                val mapSavedData = MapItem.getSavedData(MapId(mapMemory.mapId), researcher.level())
                if (mapSavedData != null) {
                    itemStack[DataComponents.MAP_ID] = MapId(mapMemory.mapId)
                } else {
                    RuinsOfGrowsseth.LOGGER.info(
                        "Tried setting res. known map data from id ${mapMemory.mapId} but was null, making new"
                    )
                }
                mapData.description?.forEach { itemStack.loreLines().add(Component.translatable(it)) }
                RuinsOfGrowsseth.LOGGER.info("Loaded map data from known map $mapMemory")
                known = true
            }
        }

        // Check fixed map positions
        if (!known) {
            var pos: BlockPos? = null
            if (mapData.x != null && mapData.z != null) {
                pos = BlockPos(mapData.x, level.getYAtXZ(mapData.x, mapData.z), mapData.z)
            } else if (mapData.fixedStructureId != null) {
                val matchingStructures = getMatchingStructures(level.registryAccess(), mapData.fixedStructureId)
                val spawnData = fixedStructureGeneration.registeredStructureSpawns.values
                    .filter { matchingStructures.contains(it.structure) }
                    .minByOrNull { it.pos.distManhattan(researcher.blockPosition()) }
                if (spawnData != null) {
                    pos = spawnData.pos
                }
            }
            if (pos != null) {
                val destination = getStructTagOrKey(mapData.structure)
                val destinationType = mapData.overrideMapIcon?.let { DestinationType.withIcon(it, registryAccess) } ?: destination.map({
                    DestinationType.auto(it, server.registryAccess())
                }, {
                    DestinationType.auto(it)
                })

                itemStack.updateMapToPos(
                    level,
                    pos,
                    scale,
                    destinationType = destinationType,
                    displayName = mapData.name,
                )
                RuinsOfGrowsseth.LOGGER.info("Res.trades: created map to pos $pos dtype $destinationType")
                mapData.description?.forEach { itemStack.loreLines().add(Component.translatable(it)) }

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

        // Locate map
        if (!known) {
            offer.setToOutOfStock() // Disable offer until found
            // Locate map if not fixed struct or pos
            itemStack.updateMapToStruct(
                level,
                destinationName = mapData.structure,
                MapLocateContext(
                    searchFromPos = researcher.blockPosition(),
                    scale = scale,
                    displayName = mapData.name,
                    skipExploredChunks = true,
                    mustContainJigsawIds = mapData.searchForJigsawIds,
                    overrideDestinationType = mapData.overrideMapIcon?.let { DestinationType.withIcon(it, level.registryAccess()) },
                )
            ).thenAccept { result ->
                if (result != null) {
                    val pos = result.first
                    RuinsOfGrowsseth.LOGGER.info("Res.trades: found map to pos $pos struct ${mapData.structure}")
                    synchronized(researcher.storedMapLocations) {
                        researcher.storedMapLocations[mapData.structure] = Researcher.MapMemory(
                            pos,
                            Either.right(result.second.unwrapKey().get()),
                            itemStack[DataComponents.MAP_ID]?.id ?: throw IllegalStateException("Map item has no id after updating! $itemStack"),
                        )
                    }
                    mapData.description?.forEach { itemStack.loreLines().add(Component.translatable(it)) }

                    offer.resetUses()
                    researcher.refreshCurrentTrades()
                } else {
                    // Update map with failed name
                    researcher.refreshCurrentTrades()
                    researcher.scheduleClearingFailedMaps()
                }
            }
            // Update map with loading name + stock change
            researcher.refreshCurrentTrades()
        }
    }

    fun offersMatch(offersA: MerchantOffers, offersB: MerchantOffers): Boolean {
        if (offersA.size != offersB.size) return false

        // loose match: ignore components etc, only check item & amount
        for (i in offersA.indices) {
            if (offersA[i].costA.item != offersB[i].costA.item) return false
            if (offersA[i].costA.count != offersB[i].costA.count) return false
            if (offersA[i].costB.item != offersB[i].costB.item) return false
            if (offersA[i].costB.count != offersB[i].costB.count) return false
            if (offersA[i].result.item != offersB[i].result.item) return false
            if (offersA[i].result.count != offersB[i].result.count) return false
        }

        return true
    }

    /**
     * Run lengthy/non efficient functions, to be used only after
     * the trade is given to the researcher to avoid repetition
     * Includes map finding and diary setting
     */
    fun finalizeTradeResult(researcher: Researcher, offer: MerchantOffer): MerchantOffer? {
        val offerOut = offer.copy()
        val result = offerOut.result
        val data = result[DataComponents.CUSTOM_DATA]?.copyTag() ?: return offerOut
        data.loadField(ResearcherItemListing.MAP_INFO_TAG, TradeItemMapInfo.CODEC)?.let { mapInfo ->
            // Until we find a way to locate specific jigsaw pieces, skip golem map if not in growsseth
            // or fixed spawn not otherwise available
            if (mapInfo.structure.contains("golem_house")) {
                val fixedSpawnExists = mapInfo.fixedStructureId?.let { fixedStructureId ->
                    val matchingStructures = getMatchingStructures(
                        researcher.level().registryAccess(), fixedStructureId
                    )
                    fixedStructureGeneration.registeredStructureSpawns.values
                        .any { matchingStructures.contains(it.structure) }
                } ?: false

                if (!fixedSpawnExists) {
                    RuinsOfGrowsseth.LOGGER.warn("Golem house map not available yet, and no fixed spawn available in world!")
                    return null
                }
            }

            if (!researcher.level().isClientSide && result[DataComponents.CUSTOM_DATA]?.contains(ResearcherItemListing.SET_MAP_TAG) != true) {
                CustomData.update(DataComponents.CUSTOM_DATA, result) { it.putBoolean(ResearcherItemListing.SET_MAP_TAG, true) }
                setTradeMapTarget(researcher, result, mapInfo, offerOut)
            }
        }
        if (data.contains(ResearcherItemListing.BOOK_TEMPLATE_TAG)) {
            BookTemplates.loadTemplate(result, data.getString(ResearcherItemListing.BOOK_TEMPLATE_TAG), edit = { withAuthor(researcher.name.string) })
        }
        return offerOut
    }
}