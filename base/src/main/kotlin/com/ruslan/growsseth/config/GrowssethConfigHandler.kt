package com.ruslan.growsseth.config

import com.filloax.fxlib.api.FxUtils
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
        CONFIGURATOR.register(com.ruslan.growsseth.config.GrowssethConfig::class.java)
        config = CONFIGURATOR.getConfig(
            com.ruslan.growsseth.config.GrowssethConfig::class.java).also { c ->
            c.load { }
            afterLoadConfig(c)
        }
    }

    private fun afterLoadConfig(config: ResourcefulConfig) {
        loadCallbacks.forEach { it(config) }

        if (com.ruslan.growsseth.config.GrowssethConfig.serverLanguage == "client") {
            val server = FxUtils.getServer()
            if (server != null && server is DedicatedServer) {
                // if in server, cannot detect language so default to specific language
                com.ruslan.growsseth.config.GrowssethConfig.serverLanguage = DEFAULT_LANGUAGE
            }
        }
    }

    fun onConfigLoad(event: (ResourcefulConfig) -> Unit) {
        loadCallbacks.add(event)
        config?.let { event(it) }
    }

    fun saveConfig() {
        config?.save() ?: throw IllegalStateException("No config loaded!")
    }
}