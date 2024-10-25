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
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import org.apache.logging.log4j.Level
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

object GrowssethClientNeo {
    fun initializeClient(event: FMLClientSetupEvent) {
        FxLib.logger.info("Initializing client...")
        GrowssethRenderers.init()
        GrowssethItemsClient.init()

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: ModContainer, _: Screen ->
                configScreen() ?: throw Exception("Config not initialized yet during loading")
            }
        }

        MOD_BUS.addListener<RegisterClientReloadListenersEvent> { ev ->
            ev.registerReloadListener(EncryptedMusicResources.KeyListener())
        }

        FORGE_BUS.addListener<ClientTickEvent.Pre> { ev ->
            GrowssethWorldPresetClient.Callbacks.onClientTick(Minecraft.getInstance())
        }

        RuinsOfGrowsseth.log(Level.INFO, "Initialized Client!")
    }

    fun configScreen(parent: Screen? = null): ConfigScreen? = ClientConfigHandler.configScreen(parent)
}