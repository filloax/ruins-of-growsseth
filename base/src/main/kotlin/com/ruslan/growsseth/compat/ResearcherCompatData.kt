package com.ruslan.growsseth.compat

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class ResearcherCompatData(
    var cobblemonLastDefeatedDate: Long = -1L,
) {
    companion object {
        val CODEC: Codec<ResearcherCompatData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.LONG.optionalFieldOf("cobblemonLastDefeatedDate", -1L).forGetter(ResearcherCompatData::cobblemonLastDefeatedDate),
        ).apply(builder, ::ResearcherCompatData) }
    }
}