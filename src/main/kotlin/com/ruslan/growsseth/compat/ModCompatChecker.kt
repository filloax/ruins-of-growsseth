package com.ruslan.growsseth.compat

import net.fabricmc.loader.api.FabricLoader

class ModCompatChecker {
    companion object {
        const val ID_LITHOSTITCHED = "lithostitched"

        val isLithoStitchedLoaded: Boolean by lazy {
            FabricLoader.getInstance().isModLoaded(ID_LITHOSTITCHED)
        }
    }
}