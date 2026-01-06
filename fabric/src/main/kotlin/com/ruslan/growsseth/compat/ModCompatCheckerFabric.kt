package com.ruslan.growsseth.compat

import net.fabricmc.loader.api.FabricLoader

class ModCompatCheckerFabric : ModCompatChecker() {
    override fun isLoaded(id: String): Boolean = FabricLoader.getInstance().isModLoaded(id)
}