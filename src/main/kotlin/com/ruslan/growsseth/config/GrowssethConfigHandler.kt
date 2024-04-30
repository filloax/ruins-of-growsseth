package com.ruslan.growsseth.config

import com.filloax.fxlib.getServer
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import net.minecraft.server.dedicated.DedicatedServer

/**
 * Wrap/contain config initialization to avoid
 * changing the main mod file if the library changes
 * initialization means between versions
 */
object GrowssethConfigHandler {
    private val CONFIGURATOR = Configurator(RuinsOfGrowsseth.MOD_ID)
    var config: ResourcefulConfig? = null
        private set
    private val loadCallbacks: MutableList<(ResourcefulConfig) -> Unit> = mutableListOf()

    val INCLUDED_LANGUAGES = setOf(
        "en_us",
        "it_it",
    )
    val DEFAULT_LANGUAGE = "en_us"

    fun initConfig() {
        CONFIGURATOR.register(GrowssethConfig::class.java)
        config = CONFIGURATOR.getConfig(GrowssethConfig::class.java).also { c ->
            c.load()
            afterLoadConfig(c)
        }
    }

    private fun afterLoadConfig(config: ResourcefulConfig) {
        loadCallbacks.forEach { it(config) }

        if (GrowssethConfig.serverLanguage == "client") {
            val server = getServer()
            if (server != null && server is DedicatedServer) {
                // if in server, cannot detect language so default to specific language
                GrowssethConfig.serverLanguage = DEFAULT_LANGUAGE
            }
        }
    }

    fun onConfigLoad(event: (ResourcefulConfig) -> Unit) {
        loadCallbacks.add(event)
        config?.let { event(it) }
    }
}