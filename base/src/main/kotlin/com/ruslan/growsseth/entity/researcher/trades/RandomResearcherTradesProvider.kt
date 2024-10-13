package com.ruslan.growsseth.entity.researcher.trades

import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.trading.MerchantOffers
import kotlin.random.Random

object RandomResearcherTradesProvider : AbstractResearcherTradesProvider() {
    override fun getOffersImpl(
        researcher: Researcher,
        tradesData: ResearcherTradesData,
        player: ServerPlayer,
    ): MerchantOffers {
        val time = researcher.level().gameTime
        val redoTrades = tradesData.randomTrades == null
                || tradesData.lastRandomTradeChangeTime < 0
                || com.ruslan.growsseth.config.ResearcherConfig.randomTradesRefreshTime > 0 && time - tradesData.lastRandomTradeChangeTime > com.ruslan.growsseth.config.ResearcherConfig.randomTradesRefreshTime * Constants.DAY_TICKS_DURATION
        val trades = if (redoTrades) {
            val out = pickTrades(researcher, player)
            tradesData.randomTrades = out
            tradesData.lastRandomTradeChangeTime = time
            out
        } else {
            tradesData.randomTrades!!
        }

        val filteredTrades = processTrades(trades)

        return MerchantOffers().apply {
            addAll(filteredTrades.map { it.itemListing.getOffer(researcher, researcher.random) })
        }
    }

    private fun pickTrades(researcher: Researcher, player: ServerPlayer): List<ResearcherTradeEntry> {
        val random = Random(researcher.random.nextInt())
        val structures = pickStructures(researcher, player)

        val maxTradesItems = TradesListener.RANDOM_TRADES_POOL.size
        var amount = com.ruslan.growsseth.config.ResearcherConfig.randomTradeNumItems.range().random(random)
        if (amount > maxTradesItems)
            amount = maxTradesItems

        return listOf(
            TradesListener.FIXED_TRADES_WHEN_RANDOM,
            TradesListener.RANDOM_TRADES_POOL.shuffled(random).subList(0, amount),
            TradesListener.TRADES_BEFORE_STRUCTURE.filterKeys { key -> structures.contains(key) }.values.flatten(),
        ).flatten()
    }

    private fun pickStructures(researcher: Researcher, player: ServerPlayer): List<String> {
        val available = TradesListener.TRADES_BEFORE_STRUCTURE.keys
        val random = Random(researcher.random.nextInt())
        var amount = com.ruslan.growsseth.config.ResearcherConfig.randomTradeNumMaps.range().random(random)
        val maxTradesMaps = available.size
        if (amount > maxTradesMaps)
            amount = available.size
        return available.shuffled(random).subList(0, amount)
    }

    override val mode = ResearcherTradeMode.RANDOM
}