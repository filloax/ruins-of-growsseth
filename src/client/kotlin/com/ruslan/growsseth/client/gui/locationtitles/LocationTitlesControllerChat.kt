package com.ruslan.growsseth.client.gui.locationtitles

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class LocationTitlesControllerChat : LocationTitlesController {

    override fun showLocationTitle(title: String) {
        val client = Minecraft.getInstance()
        client.chatListener.handleSystemMessage(Component.translatable(title), true)
    }

    override fun isShowingTitle(): Boolean {
        return false
    }
}