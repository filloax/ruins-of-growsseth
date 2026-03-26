package com.ruslan.growsseth.compat

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class ResearcherCompatData(
    var cobblemonLastDefeatedDate: Long = -1L,
    var cobblemonDefeatedOnce: Boolean = false,
    var cobblemonUsedMaxLevelOnce: Boolean = false,
) {
    companion object {
        val CODEC: Codec<ResearcherCompatData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.LONG.optionalFieldOf("cobblemonLastDefeatedDate", -1L).forGetter(ResearcherCompatData::cobblemonLastDefeatedDate),
            Codec.BOOL.optionalFieldOf("cobblemonDefeatedOnce", false).forGetter(ResearcherCompatData::cobblemonDefeatedOnce),
            Codec.BOOL.optionalFieldOf("cobblemonUsedMaxLevelOnce", false).forGetter(ResearcherCompatData::cobblemonUsedMaxLevelOnce),
        ).apply(builder, ::ResearcherCompatData) }
    }
}