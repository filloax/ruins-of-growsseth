package com.ruslan.growsseth.entity.researcher.trades

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
        val trades = tradesData.randomTrades ?: run {
            val out = pickTrades(researcher, player)
            tradesData.randomTrades = out
            out
        }
        val filteredTrades = processTrades(trades)

        return MerchantOffers().apply {
            addAll(filteredTrades.map { it.itemListing.getOffer(researcher, researcher.random) })
        }
    }

    private fun pickTrades(researcher: Researcher, player: ServerPlayer): List<ResearcherTradeEntry> {
        val random = Random(player.server.overworld().seed * (researcher.persistId ?: 1))
        val amount = IntRange(ResearcherConfig.randomTradeNumItems.min, ResearcherConfig.randomTradeNumItems.max).random(random)
        return listOf(
            TradesListener.FIXED_TRADES_WHEN_RANDOM,
            TradesListener.RANDOM_TRADES_POOL.shuffled(random).subList(0, amount),
        ).flatten()
    }

    override val mode = ResearcherTradeMode.RANDOM
}