package com.ruslan.growsseth.config

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig

/**
 * Wrap/contain config initialization to avoid
 * changing the main mod file if the library changes
 * initialization means between versions
 */
object GrowssethConfigHandler {
    private val CONFIGURATOR = Configurator(RuinsOfGrowsseth.MOD_ID)
    var config: ResourcefulConfig? = null
        private set

    fun initConfig() {
        CONFIGURATOR.register(GrowssethConfig::class.java)
        config = CONFIGURATOR.getConfig(GrowssethConfig::class.java)
    }
}