package com.ruslan.growsseth.config

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.teamresourceful.resourcefulconfig.client.ConfigScreen
import net.minecraft.client.gui.screens.Screen

object ClientConfigHandler {
    fun configScreen(parent: Screen?): ConfigScreen? {
        val config = GrowssethConfigHandler.config
        return config?.let{ ConfigScreen(parent, config) }
    }
}