package com.ruslan.growsseth.entity.researcher.trades

import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.trading.MerchantOffers
import java.util.UUID

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
    var lastPlayers = mutableMapOf<UUID, UUID>()

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


    private fun getPlayer(researcher: Researcher): ServerPlayer? {
        val server = researcher.server ?: throw IllegalStateException("Used AbstractGameProvider from client thread!")
        val playerUuid = lastPlayers[researcher.uuid]
        return playerUuid?.let { server.playerList.getPlayer(playerUuid) }
    }
}