package com.ruslan.growsseth

import com.ruslan.growsseth.client.GrowssethItemsClient
import com.ruslan.growsseth.client.GrowssethRenderers
import com.ruslan.growsseth.client.network.GrowssethNetworkingClient
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.config.ClientConfigHandler
import com.teamresourceful.resourcefulconfig.client.ConfigScreen

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.Level


object GrowssethClient : ClientModInitializer {
    override fun onInitializeClient() {
        GrowssethRenderers.init()
        GrowssethNetworkingClient.init()
        GrowssethItemsClient.init()

        initEvents()

        RuinsOfGrowsseth.log(Level.INFO, "Initialized Client!")
    }

    private fun initEvents() {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            GrowssethWorldPresetClient.Callbacks.onClientTick(client)
        }
    }

    @JvmStatic
    fun configScreen(parent: Screen? = null): ConfigScreen? = ClientConfigHandler.configScreen(parent)
}