package com.ruslan.growsseth.entity.researcher.trades

import com.mojang.serialization.Codec
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.minecraft.server.MinecraftServer
import net.minecraft.util.StringRepresentable

enum class ResearcherTradeMode(val id: String) : StringRepresentable {
    // single researcher on AND random trades off
    PROGRESS("progress"),
    // single researcher off OR random trades on
    RANDOM("random"),
    // regardless of prev settings, in growsseth world gen
    GROWSSETH_PROGRESS("growsseth_progress"),
    // regardless of any setting, when game master mode is on (data sync) and webTrades on
    GAME_MASTER("game_master"),
    ;

    companion object {
        val CODEC: Codec<ResearcherTradeMode> = StringRepresentable.fromEnum(ResearcherTradeMode::values)
        val PROVIDERS = mapOf(
            RANDOM to RandomResearcherTradesProvider,
            GAME_MASTER to GameMasterResearcherTradesProvider,
            PROGRESS to ProgressResearcherTradesProvider(GrowssethStructures.ORIGINAL_STRUCTURES),
            GROWSSETH_PROGRESS to ProgressResearcherTradesProvider(GrowssethStructures.ORIGINAL_STRUCTURES, inOrder = true),
        )

        fun getFromSettings(server: MinecraftServer) = if (WebConfig.webDataSync && ResearcherConfig.webTrades) {
                GAME_MASTER
            } else if (GrowssethWorldPreset.isGrowssethPreset(server)) {
                GROWSSETH_PROGRESS
            } else if (!ResearcherConfig.singleResearcher || !ResearcherConfig.singleResearcherProgress) {
                RANDOM
            } else {
                PROGRESS
            }

        fun providerFromSettings(server: MinecraftServer) = PROVIDERS[getFromSettings(server)]!!
    }

    override fun getSerializedName(): String {
        return this.id
    }
}