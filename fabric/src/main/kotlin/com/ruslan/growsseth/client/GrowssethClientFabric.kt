package com.ruslan.growsseth.client

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.client.resource.EncryptedMusicResources
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.config.ClientConfigHandler
import com.teamresourceful.resourcefulconfig.client.ConfigScreen

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.gui.screens.Screen
import net.minecraft.server.packs.PackType
import org.apache.logging.log4j.Level


object GrowssethClientFabric : ClientModInitializer {
    override fun onInitializeClient() {
        GrowssethRenderers.init()
        GrowssethItemsClient.init()

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(EncryptedMusicResources.KeyListener() as IdentifiableResourceReloadListener)

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