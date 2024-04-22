package com.ruslan.growsseth.entity.researcher.trades

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.structure.Structure

/**
 * If no preset order, unlock structures in random order (avoiding already discovered ones)
 */
class ProgressResearcherTradesProvider(
    private val order: List<ResourceKey<Structure>>? = null,
) : GlobalResearcherTradesProvider() {
    override val mode: ResearcherTradeMode = if (order != null)  ResearcherTradeMode.GROWSSETH_PROGRESS else ResearcherTradeMode.PROGRESS

    companion object {

    }
}