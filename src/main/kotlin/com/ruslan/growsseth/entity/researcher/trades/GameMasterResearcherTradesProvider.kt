package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.*
import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.getStructTagOrKey
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.http.ApiEvent
import com.ruslan.growsseth.http.DataRemoteSync
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.structure.RemoteStructures
import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object GameMasterResearcherTradesProvider : GlobalResearcherTradesProvider() {
    private val JSON = Json { isLenient = true }

    override val mode: ResearcherTradeMode = ResearcherTradeMode.GAME_MASTER

    init {
        init()
    }

    fun subscribe() {
        GrowssethApi.current.subscribe { api, server ->
            EventUtil.runWhenServerStarted(server) { server2 ->
                updateTrades(server2, api)
            }
        }
    }

    override fun isValidTradeForPlayer(trade: ResearcherItemListing, player: ServerPlayer, entity: Researcher, data: ResearcherTradesData): Boolean {
        return !trade.isDiscoveredStructure(player)
    }

    override fun reload(server: MinecraftServer) {
        GrowssethApi.current.reload()
    }

    private fun updateTrades(server: MinecraftServer, api: GrowssethApi = GrowssethApi.current) {
        val workingList = mutableListOf<ResearcherTradeEntry>()
        workingList.addAll(getUnlockedTradesByRemoteStruct())
        workingList.addAll(getUnlockedTradesByRemoteEvent(api.events))
        workingList.addAll(getRemoteCustomTrades(api.events))

        applyUpdatedTrades(server, workingList)
    }

    private fun getUnlockedTradesByRemoteStruct(): List<ResearcherTradeEntry> {
        val spawnsById = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID
        val unlockableTrades = TradesListener.WEB_UNLOCKABLE_TRADES_BY_STRUCT
        return unlockableTrades.filter { (id, _) -> spawnsById.any { (spawnId, structureSpawn) ->
            structureSpawn.structure.path == id && fixedStructureGeneration.spawnedQueuedStructure(spawnId) == false
        } }.flatMap { it.value }
    }

    private fun getUnlockedTradesByRemoteEvent(events: List<ApiEvent>): List<ResearcherTradeEntry> {
        val unlockableTrades = TradesListener.WEB_UNLOCKABLE_TRADES_BY_EVENT
        return unlockableTrades.filter { (name, trades) -> events.any { event ->
            event.active && event.name == name
        } }.flatMap { it.value }
    }

    private fun getRemoteCustomTrades(events: List<ApiEvent>): List<ResearcherTradeEntry> {
        val eventTradeInfos = events.filter { event -> event.name.startsWith("customTrade/") && event.active }
        return eventTradeInfos.mapNotNull { tradeEvent ->
            val desc = tradeEvent.desc ?: run {
                RuinsOfGrowsseth.LOGGER.error("Custom trade event $tradeEvent doesn't have a desc")
                return@mapNotNull null
            }
            try {
                JSON.decodeFromString(ResearcherTradeObj.serializer(), desc).copy(maxUses = 999).decode()
            } catch (e: Exception) {
                e.printStackTrace()
                RuinsOfGrowsseth.LOGGER.error("Custom trade event $tradeEvent has wrongly formatted JSON trade data")
                null
            }
        }
    }
}

