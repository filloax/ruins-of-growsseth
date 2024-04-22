package com.ruslan.growsseth.entity.researcher.trades

import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.levelgen.structure.Structure

/**
 * Contains data for researcher trades with the various options,
 * tracked separately so for instance if random trades are enabled, then disabled,
 * then re-enabled, the researcher entity will keep the same ones.
 */
class ResearcherTradesData (
    var mode: ResearcherTradeMode,
    randomTrades: List<ResearcherTradeEntry>? = null,
    val foundStructures: MutableSet<ResourceKey<Structure>> = mutableSetOf(),
) {
    var randomTrades: List<ResearcherTradeEntry>? = randomTrades
        set(value) {
            if (field != null) {
                throw IllegalStateException("Cannot set randomTrades once initialized from null")
            }
            field = value
        }

    companion object {

    }
}