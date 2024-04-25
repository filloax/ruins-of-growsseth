package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.codec.forNullableGetter
import com.filloax.fxlib.codec.mutableListCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Contains data for researcher trades with the various options,
 * tracked separately so for instance if random trades are enabled, then disabled,
 * then re-enabled, the researcher entity will keep the same ones.
 */
class ResearcherTradesData (
    var mode: ResearcherTradeMode,
    randomTrades: Optional<List<ResearcherTradeEntry>> = Optional.empty(),
    // Used to refresh trade uses
    // In ticks (remember 24000 is 1 day (or equivalent with daytime off))
    var lastTradeRefreshTime: Long = -1L,
    // Used to change random trades when enabled by mode or setting, in ticks
    var lastRandomTradeChangeTime: Long = -1L,
    val lastAvailableRandomTrades: MutableList<Holder<Item>> = mutableListOf(),
) {
    var randomTrades: List<ResearcherTradeEntry>? = randomTrades.getOrNull()

    companion object {
        val CODEC: Codec<ResearcherTradesData> = RecordCodecBuilder.create { builder -> builder.group(
            ResearcherTradeMode.CODEC.fieldOf("mode").forGetter(ResearcherTradesData::mode),
            ResearcherTradeEntry.CODEC.listOf().optionalFieldOf("randomTrades").forNullableGetter(ResearcherTradesData::randomTrades),
            Codec.LONG.optionalFieldOf("lastTradeRefreshTime", -1L).forGetter(ResearcherTradesData::lastTradeRefreshTime),
            Codec.LONG.optionalFieldOf("lastRandomTradeChangeTime", -1L).forGetter(ResearcherTradesData::lastRandomTradeChangeTime),
            mutableListCodec(BuiltInRegistries.ITEM.holderByNameCodec()).optionalFieldOf("lastAvailableRandomTrades", mutableListOf()).forGetter(ResearcherTradesData::lastAvailableRandomTrades),
        ).apply(builder, ::ResearcherTradesData) }
    }

    fun resetRandomTrades() { lastRandomTradeChangeTime = -1 }
}