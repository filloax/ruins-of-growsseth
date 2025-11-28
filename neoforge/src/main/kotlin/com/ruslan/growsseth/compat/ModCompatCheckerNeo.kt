package com.ruslan.growsseth.compat

import net.neoforged.fml.ModList

class ModCompatCheckerNeo : ModCompatChecker {
    override val isLithostitchedLoaded: Boolean by lazy {
        ModList.get().isLoaded(ModCompatChecker.ID_LITHOSTITCHED)
    }
    override val isImprovedVillagePlacementLoaded: Boolean by lazy {
        ModList.get().isLoaded(ModCompatChecker.ID_IMPROVED_VILLAGE_PLACEMENT)
    }
    override val isCobblemonLoaded: Boolean by lazy {
        ModList.get().isLoaded(ModCompatChecker.ID_COBBLEMON)
    }
    override val isRCTTrainerApiLoaded: Boolean by lazy {
        ModList.get().isLoaded(ModCompatChecker.ID_RCT_TRAINER_API)
    }
}