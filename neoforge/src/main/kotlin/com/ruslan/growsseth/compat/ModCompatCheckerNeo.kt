package com.ruslan.growsseth.compat

import net.neoforged.fml.ModList

class ModCompatCheckerNeo : ModCompatChecker() {
    override fun isLoaded(id: String): Boolean = ModList.get().isLoaded(id)
}