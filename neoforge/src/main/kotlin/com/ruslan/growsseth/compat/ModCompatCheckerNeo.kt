package com.ruslan.growsseth.compat

import net.neoforged.fml.ModList

class ModCompatCheckerNeo : ModCompatChecker {
    override val isLithoStitchedLoaded: Boolean by lazy {
        ModList.get().isLoaded(ModCompatChecker.ID_LITHOSTITCHED)
    }
}