package com.ruslan.growsseth.compat

import net.fabricmc.loader.api.FabricLoader

class ModCompatCheckerFabric : ModCompatChecker {
    override val isLithostitchedLoaded: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_LITHOSTITCHED)
    }
    override val isImprovedVillagePlacementLoaded: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_IMPROVED_VILLAGE_PLACEMENT)
    }
}