package com.ruslan.growsseth.config

import com.teamresourceful.resourcefulconfig.client.ConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ClientConfigHandler {
    private val SET_LANGUAGE_TOAST_ID = SystemToastId(3000)

    fun configScreen(parent: Screen?): ConfigScreen? {
        val config = GrowssethConfigHandler.config
        return config?.let{ ConfigScreen(parent, config) }
    }

    @JvmStatic
    fun setServerLangFromClient() {
        val client = Minecraft.getInstance()
        val lang = client.languageManager.selected
        GrowssethConfig.serverLanguage = if (GrowssethConfigHandler.INCLUDED_LANGUAGES.contains(lang))
            lang
        else
            GrowssethConfigHandler.DEFAULT_LANGUAGE

        GrowssethConfigHandler.config?.save()       // Need to save manually to config file

        client.toasts.addToast(SystemToast.multiline(
            client, SET_LANGUAGE_TOAST_ID,
            Component.translatable("growsseth.notif.setLanguage.title"),
            Component.translatable("growsseth.notif.setLanguage.message", GrowssethConfig.serverLanguage),
        ))
    }
}