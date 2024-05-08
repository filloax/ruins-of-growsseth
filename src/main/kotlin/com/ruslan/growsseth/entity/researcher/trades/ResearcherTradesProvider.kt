package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.FxLibServices
import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.trading.MerchantOffers
import java.util.*

/**
 * Server-side
 */
interface ResearcherTradesProvider {
    /**
     * Used so Researcher can properly implement Merchant interface as it has no player
     */
    fun getOffers(researcher: Researcher, tradesData: ResearcherTradesData): MerchantOffers
    fun getOffers(researcher: Researcher, tradesData: ResearcherTradesData, player: ServerPlayer): MerchantOffers

    val mode: ResearcherTradeMode
}

abstract class AbstractResearcherTradesProvider : ResearcherTradesProvider {
    // simply cache players to respond to the player-less version of getOffers
    private var lastPlayers = mutableMapOf<UUID, UUID>()
    protected val fixedStructureGeneration = FxLibServices.fixedStructureGeneration

    final override fun getOffers(researcher: Researcher, tradesData: ResearcherTradesData): MerchantOffers {
        val player = getPlayer(researcher) ?: throw IllegalStateException("Tried running Researcher getOffers before any player used it!")
        return getOffers(researcher, tradesData, player)
    }

    final override fun getOffers(
        researcher: Researcher,
        tradesData: ResearcherTradesData,
        player: ServerPlayer
    ): MerchantOffers {
        lastPlayers[researcher.uuid] = player.uuid
        return getOffersImpl(researcher, tradesData, player)
    }

    abstract fun getOffersImpl(researcher: Researcher, tradesData: ResearcherTradesData, player: ServerPlayer): MerchantOffers

    protected fun isEnabled(server: MinecraftServer) = ResearcherTradeMode.getFromSettings(server) == mode

    /** Apply the replaces parameter and sort by priority */
    protected fun processTrades(list: List<ResearcherTradeEntry>): List<ResearcherTradeEntry> {
        // Make a list in case no trade with that item has replaces, and so all are kept
        val valueToItemMap = mutableMapOf<Item, MutableList<ResearcherTradeEntry>>()

        for (entry in list) {
            val value = entry.itemListing.gives.item
            val itemList = valueToItemMap.computeIfAbsent(value) { mutableListOf() }
            if (entry.replace) {
                // Priority affects order such that low priority number goes first, so treat it the same here
                itemList.removeAll { it.priority > entry.priority || !it.replace }
            }
            itemList.add(entry)
        }

        return valueToItemMap.values.flatten().sortedBy { it.priority }
    }

    private fun getPlayer(researcher: Researcher): ServerPlayer? {
        val server = researcher.server ?: throw IllegalStateException("Used AbstractGameProvider from client thread!")
        val playerUuid = lastPlayers[researcher.uuid]
        return playerUuid?.let { server.playerList.getPlayer(playerUuid) }
    }
}