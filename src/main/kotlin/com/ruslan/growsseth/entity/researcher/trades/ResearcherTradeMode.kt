package com.ruslan.growsseth.entity.researcher.trades

import com.mojang.serialization.Codec
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Keyable
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.trading.MerchantOffers
import java.util.stream.Stream

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
            PROGRESS to ProgressResearcherTradesProvider(listOf(
                GrowssethStructures.CAVE_CAMP,
                GrowssethStructures.GOLEM_HOUSE,
                GrowssethStructures.ENCHANT_TOWER,
                GrowssethStructures.NOTEBLOCK_LAB,
                GrowssethStructures.BEEKEEPER_HOUSE,
                GrowssethStructures.CONDUIT_RUINS,
            )),
            GROWSSETH_PROGRESS to ProgressResearcherTradesProvider(listOf(
                GrowssethStructures.CAVE_CAMP,
                GrowssethStructures.GOLEM_HOUSE,
                GrowssethStructures.ENCHANT_TOWER,
                GrowssethStructures.NOTEBLOCK_LAB,
                GrowssethStructures.BEEKEEPER_HOUSE,
                GrowssethStructures.CONDUIT_RUINS,
            ), inOrder = true),
        )

        fun getFromSettings(server: MinecraftServer) = if (WebConfig.webDataSync && ResearcherConfig.webTrades) {
                GAME_MASTER
            } else if (GrowssethWorldPreset.isGrowssethPreset(server)) {
                GROWSSETH_PROGRESS
            } else if (!ResearcherConfig.singleResearcher || !ResearcherConfig.singleResearcherProgressTrades) {
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