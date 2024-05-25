package com.ruslan.growsseth.data

import com.filloax.fxlib.api.json.saveStable
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTradeObj
import com.ruslan.growsseth.entity.researcher.trades.TradeItemMapInfo
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.VillageBuildings
import kotlinx.serialization.json.Json
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Items
import net.minecraft.world.item.RecordItem
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createDirectories

class CustomDataProvider(private val output: PackOutput) : DataProvider {
    val json = Json {
        prettyPrint = true
    }

    override fun run(writer: CachedOutput): CompletableFuture<*> {
        return CompletableFuture.allOf(
            generateResearcherTrades(writer),
        )
    }

    private fun generateResearcherTrades(writer: CachedOutput): CompletableFuture<*> {
        val tradesObj = TradesListener.TradesObj(
            generateResearcherTradesFixedWhenRandom(),
            generateResearcherTradesByStructure(),
            generateResearcherTradesByEvent(),
            generateResearcherTradesBeforeStruct(),
            generateResearcherTradesProgressAfterStruct(),
            generateResearcherTradesProgressAfterStructRandom(),
            generateResearcherTradesRandomTrades(),
        )

        val outputFolder = output.getOutputFolder(PackOutput.Target.DATA_PACK)
            .resolve(RuinsOfGrowsseth.MOD_ID)
            .resolve(Constants.TRADES_DATA_FOLDER)
        outputFolder.createDirectories()

        val filename = "generated_trades.json"
        return saveStable(writer, TradesListener.TradesObj.serializer(), tradesObj, outputFolder.resolve(filename))
    }

    private fun generateResearcherTradesRandomTrades(): List<ResearcherTradeObj> {
        val out = mutableListOf<ResearcherTradeObj>()
        val validDiscs = GrowssethItems.all.filterValues { it is RecordItem && !GrowssethItems.DISCS_TO_VOCALS.values.contains(it) }
        out.addAll(validDiscs.map {
            ResearcherTradeObj(
                ResearcherTradeObj.tradeItemEntryObj(it.key, 1),
                listOf(ResearcherTradeObj.tradeItemEntryObj(Items.EMERALD, 5)),
                randomWeight = 1f / validDiscs.size
            )
        })
        return out
    }

    private fun generateResearcherTradesBeforeStruct(): Map<String, List<ResearcherTradeObj>> {
        val out = mutableMapOf<String, List<ResearcherTradeObj>>()

        val makeStructTrades = { key: ResourceKey<Structure>, modify: ((ResearcherTradeObj.TradeItemEntryObj) -> ResearcherTradeObj.TradeItemEntryObj)? ->
            val info = GrowssethStructures.info[key]!!
            val tagString = "#${info.tag.location}"
            listOf(
                ResearcherTradeObj(
                    ResearcherTradeObj.tradeItemEntryObj(GrowssethItems.RUINS_MAP, map = TradeItemMapInfo.JsonDesc(
                        structure = tagString,
                        name = "structure.${key.location().toLanguageKey()}.map.name",
                        fixedStructureId = tagString,
                    )).let { obj -> modify?.let{ it(obj) } ?: obj },
                    listOf(
                        if (info.emeraldCost > 0) {
                            ResearcherTradeObj.tradeItemEntryObj(Items.EMERALD, info.emeraldCost)
                        } else {
                            ResearcherTradeObj.tradeItemEntryObj(Items.MAP, 1)
                        },
                    ),
                    priority = -50,
                )
            )
        }

        GrowssethStructures.PROGRESS_STRUCTURES
            .minus(GrowssethStructures.GOLEM_HOUSE)
            .forEach { key -> out[key.location().path] = makeStructTrades(key, null) }
        GrowssethStructures.GOLEM_HOUSE.let { key ->
            out[key.location().path] = makeStructTrades(key) { obj ->
                val associatedJigsaws = GrowssethStructures.VILLAGE_HOUSE_STRUCTURES[key]!!
                val maps = associatedJigsaws.map { (villageKey, houseIds) ->
                    TradeItemMapInfo.JsonDesc(
                        structure = villageKey.location().toString(),
                        name = "structure.${key.location().toLanguageKey()}.map.name",
                        fixedStructureId = key.location().toString(),
                        overrideMapIcon = GrowssethMapDecorations.GOLEM_HOUSE.unwrapKey().orElseThrow().location(),
                        searchForJigsawIds = houseIds
                    )
                }
                obj.copy(mapPool = maps)
            }
        }
        return out
    }

    private fun generateResearcherTradesProgressAfterStructRandom(): Map<String, List<ResearcherTradeObj>> {
        val discTrades = mutableListOf<ResearcherTradeObj>()
        val validDiscs = GrowssethItems.all.filterValues { it is RecordItem && !GrowssethItems.DISCS_TO_VOCALS.values.contains(it) }
            // discs already in guaranteed trade
            .filterValues { it != GrowssethItems.DISC_GIORGIO_LOFI_INST && it != GrowssethItems.DISC_GIORGIO_CUBETTI }
        discTrades.addAll(validDiscs.map {
            ResearcherTradeObj(
                ResearcherTradeObj.tradeItemEntryObj(it.key, 1),
                listOf(ResearcherTradeObj.tradeItemEntryObj(Items.EMERALD, 5)),
                randomWeight = 1f / validDiscs.size
            )
        })

        return mapOf(
            GrowssethStructures.NOTEBLOCK_LAB.location().path to discTrades
        )
    }

    private fun generateResearcherTradesFixedWhenRandom() = listOf<ResearcherTradeObj>()
    private fun generateResearcherTradesByStructure() = mapOf<String, List<ResearcherTradeObj>>()
    private fun generateResearcherTradesByEvent() = mapOf<String, List<ResearcherTradeObj>>()
    private fun generateResearcherTradesProgressAfterStruct() = mapOf<String, List<ResearcherTradeObj>>()

    /**
     * Gets a name for this provider, to use in logging.
     */
    override fun getName(): String = "CustomDataProvider"

}