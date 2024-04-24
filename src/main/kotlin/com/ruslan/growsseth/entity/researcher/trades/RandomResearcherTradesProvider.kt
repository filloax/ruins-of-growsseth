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
        data: ResearcherTradesData,
        player: ServerPlayer,
    ): MerchantOffers {
        val time = researcher.level().gameTime
        val redoTrades = data.randomTrades == null
                || data.lastRandomTradeChangeTime < 0
                || ResearcherConfig.randomTradesRefreshTime > 0 && time - data.lastRandomTradeChangeTime > ResearcherConfig.randomTradesRefreshTime
        val trades = if (redoTrades) {
            val out = pickTrades(researcher, player)
            data.randomTrades = out
            data.lastRandomTradeChangeTime = time
            out
        } else {
            data.randomTrades!!
        }

        val filteredTrades = processTrades(trades)

        return MerchantOffers().apply {
            addAll(filteredTrades.map { it.itemListing.getOffer(researcher, researcher.random) })
        }
    }

    private fun pickTrades(researcher: Researcher, player: ServerPlayer): List<ResearcherTradeEntry> {
        val random = Random(researcher.random.nextInt())

        val structures = pickStructures(researcher, player)

        val amount = ResearcherConfig.randomTradeNumItems.range().random(random)
        return listOf(
            TradesListener.FIXED_TRADES_WHEN_RANDOM,
            TradesListener.RANDOM_TRADES_POOL.shuffled(random).subList(0, amount),
            TradesListener.TRADES_BEFORE_STRUCTURE.filterKeys { key -> structures.contains(key) }.values.flatten(),
        ).flatten()
    }

    private fun pickStructures(researcher: Researcher, player: ServerPlayer): List<String> {
        val available = TradesListener.TRADES_BEFORE_STRUCTURE.keys
        val random = Random(researcher.random.nextInt())
        val amount = ResearcherConfig.randomTradeNumMaps.range().random(random)
        return available.shuffled(random).subList(0, amount)
    }

    override val mode = ResearcherTradeMode.RANDOM
}