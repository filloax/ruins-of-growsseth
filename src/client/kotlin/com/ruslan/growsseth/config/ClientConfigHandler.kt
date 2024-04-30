package com.ruslan.growsseth.config

import com.teamresourceful.resourcefulconfig.client.ConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object ClientConfigHandler {
    fun configScreen(parent: Screen?): ConfigScreen? {
        val config = GrowssethConfigHandler.config
        return config?.let{ ConfigScreen(parent, config) }
    }

    fun initClientConfig() {
        GrowssethConfigHandler.onConfigLoad {
            if (GrowssethConfig.serverLanguage == "client") {
                val lang = Minecraft.getInstance().languageManager.selected
                GrowssethConfig.serverLanguage = if (GrowssethConfigHandler.INCLUDED_LANGUAGES.contains(lang))
                    lang
                else
                    GrowssethConfigHandler.DEFAULT_LANGUAGE
            }
        }
    }
}