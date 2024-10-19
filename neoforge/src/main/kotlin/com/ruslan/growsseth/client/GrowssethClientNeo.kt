package com.ruslan.growsseth.client

import com.filloax.fxlib.FxLib
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ClientConfigHandler
import com.teamresourceful.resourcefulconfig.client.ConfigScreen

import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.Level


object GrowssethClientNeo {
    fun initializeClient(event: FMLClientSetupEvent) {
        FxLib.logger.info("Initializing client...")
        GrowssethRenderers.init()
        GrowssethItemsClient.init()

        // TODO
//        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(EncryptedMusicResources.KeyListener() as IdentifiableResourceReloadListener)

        initEvents()

        RuinsOfGrowsseth.log(Level.INFO, "Initialized Client!")
    }

    private fun initEvents() {
        // TODO
//        ClientTickEvents.START_CLIENT_TICK.register { client ->
//            GrowssethWorldPresetClient.Callbacks.onClientTick(client)
//        }
    }

    @JvmStatic
    fun configScreen(parent: Screen? = null): ConfigScreen? = ClientConfigHandler.configScreen(parent)
}