package com.ruslan.growsseth.compat

import net.fabricmc.loader.api.FabricLoader

class ModCompatCheckerFabric : ModCompatChecker {
    override val isLithoStitchedLoaded: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_LITHOSTITCHED)
    }
}