package com.ruslan.growsseth.client

import com.filloax.fxlib.FxLib
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.client.resource.EncryptedMusicResources
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.config.ClientConfigHandler
import com.teamresourceful.resourcefulconfig.client.ConfigScreen
import net.minecraft.client.Minecraft

import net.minecraft.client.gui.screens.Screen
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import org.apache.logging.log4j.Level

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object GrowssethClientNeo {
    fun initializeClient(event: FMLClientSetupEvent) {
        FxLib.logger.info("Initializing client...")
        GrowssethRenderers.init()
        GrowssethItemsClient.init()

        RuinsOfGrowsseth.log(Level.INFO, "Initialized Client!")
    }

    @SubscribeEvent
    fun registerResourceReloadListeners(ev: RegisterClientReloadListenersEvent) {
        ev.registerReloadListener(EncryptedMusicResources.KeyListener())
    }

    @SubscribeEvent
    fun clientTickEventPre(ev: ClientTickEvent.Pre) {
        GrowssethWorldPresetClient.Callbacks.onClientTick(Minecraft.getInstance())
    }

    @JvmStatic
    fun configScreen(parent: Screen? = null): ConfigScreen? = ClientConfigHandler.configScreen(parent)
}