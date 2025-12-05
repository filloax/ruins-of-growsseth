package com.ruslan.growsseth

import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.attachments.GrowssethAttachmentsNeo
import com.ruslan.growsseth.client.GrowssethItemsClient
import com.ruslan.growsseth.client.GrowssethRenderers
import com.ruslan.growsseth.client.resource.EncryptedMusicResources
import com.ruslan.growsseth.client.worldpreset.GrowssethWorldPresetClient
import com.ruslan.growsseth.compat.cobblemon.CobblemonRCTListener
import com.ruslan.growsseth.config.ClientConfigHandler
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.structure.GrowssethStructurePieceTypes
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.templates.TemplateListener
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist


@Mod(RuinsOfGrowsseth.MOD_ID)
object RuinsOfGrowssethNeo : RuinsOfGrowsseth() {
    init {
        isNeoforge = true
        initialize()

        runForDist(
            clientTarget = {
                initializeClient()
                MOD_BUS.addListener(::setupClient)
                Minecraft.getInstance()
            },
            serverTarget = {}
        )
    }

    override fun initItemGroups() {
        MOD_BUS.addListener<BuildCreativeModeTabContentsEvent> { ev ->
            val entries = ev.searchEntries.associateBy { it.item }
            val addAfter = { item: Item, new: Item ->
                ev.insertAfter(entries[item]!!, new.defaultInstance, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)
            }

            when (ev.tabKey) {
                CreativeModeTabs.INGREDIENTS -> {
                    addAfter(Items.PIGLIN_BANNER_PATTERN, GrowssethItems.GROWSSETH_BANNER_PATTERN)
                    addAfter(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, GrowssethItems.GROWSSETH_ARMOR_TRIM)
                    addAfter(Items.HEART_POTTERY_SHERD, GrowssethItems.GROWSSETH_POTTERY_SHERD)
                    addAfter(Items.DISC_FRAGMENT_5, GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
                }
                CreativeModeTabs.SPAWN_EGGS -> {
                    ev.accept(GrowssethItems.RESEARCHER_SPAWN_EGG)
                    ev.accept(GrowssethItems.ZOMBIE_RESEARCHER_SPAWN_EGG)
                }
                CreativeModeTabs.COMBAT -> {
                    addAfter(Items.TRIDENT, GrowssethItems.RESEARCHER_DAGGER)
                }
                CreativeModeTabs.TOOLS_AND_UTILITIES -> {
                    addAfter(Items.GOAT_HORN, GrowssethItems.RESEARCHER_HORN)
                    for (disc in GrowssethItems.DISCS_ORDERED) {
                        ev.accept(disc)
                    }
                }
            }
        }
    }

    override fun registerResourceListeners() {
        FORGE_BUS.addListener<AddReloadListenerEvent> { ev ->
            ev.addListener(TradesListener())
            ev.addListener(ResearcherDialogueListener())
            ev.addListener(TemplateListener)
            ev.addListener(LocationNotifListener())
            if (modCompat.isAllCobblemonDepsLoaded) {
                ev.addListener(CobblemonRCTListener)
            }
        }
    }

    override fun initRegistries() {
        MOD_BUS.addListener<RegisterEvent> { ev ->
            ev.register(Registries.CREATIVE_MODE_TAB, GrowssethCreativeModeTabs::registerCreativeModeTabs)
            ev.register(Registries.ITEM, GrowssethItems::registerItems)
            ev.register(Registries.INSTRUMENT, GrowssethItems.Instruments::registerInstruments)
            ev.register(Registries.DECORATED_POT_PATTERN, GrowssethItems.SherdPatterns::registerPotPatterns)
            ev.registerHolder(Registries.MAP_DECORATION_TYPE, GrowssethMapDecorations::registerMapDecorations)
            ev.registerHolder(Registries.MOB_EFFECT, GrowssethEffects::registerEffects)
            ev.register(Registries.ENTITY_TYPE, GrowssethEntities::registerEntityTypes)
            ev.register(Registries.STRUCTURE_PIECE, GrowssethStructurePieceTypes::registerStructurePieces)
            ev.register(Registries.STRUCTURE_TYPE, GrowssethStructures::registerStructureTypes)
            ev.register(Registries.TRIGGER_TYPE, GrowssethCriterions::registerCriterions)
            if (ev.registryKey == Registries.COMMAND_ARGUMENT_TYPE) {
                GrowssethCommands.ArgumentTypes.registerArgumentTypes(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)
            }
        }

        GrowssethAttachmentsNeo.registerAll(MOD_BUS)

        FORGE_BUS.addListener<RegisterCommandsEvent> { ev ->
            GrowssethCommands.register(ev.dispatcher, ev.buildContext, ev.commandSelection)
        }
    }

    private fun registerAttachments() {
        MOD_BUS.addListener<RegisterEvent> { ev ->
        }
    }


    private fun initializeClient() {
        LOGGER.info("Initializing client...")

        GrowssethRenderers.init()

        MOD_BUS.addListener<RegisterClientReloadListenersEvent> { ev ->
            ev.registerReloadListener(EncryptedMusicResources.KeyListener())
        }

        FORGE_BUS.addListener<ClientTickEvent.Pre> { ev ->
            GrowssethWorldPresetClient.Callbacks.onClientTick(Minecraft.getInstance())
        }

        LOGGER.info("Initialized Client!")
    }

    private fun setupClient(event: FMLClientSetupEvent) {
        LOGGER.info("Setting up client...")

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: ModContainer, parent: Screen ->
                ClientConfigHandler.configScreen(parent) ?: throw Exception("Config not initialized yet during loading")
            }
        }

        GrowssethItemsClient.init()

        LOGGER.info("Setup Client!")
    }

    private fun <T : Any> RegisterEvent.register(registryKey: ResourceKey<Registry<T>>, registratorConsumer: (registrator: (ResourceLocation, T) -> Unit) -> Unit) {
        register(registryKey) { registry -> registratorConsumer(registry::register) }
    }
    private fun <T : Any> RegisterEvent.registerHolder(registryKey: ResourceKey<Registry<T>>, registratorConsumer: (registrator: (ResourceLocation, T) -> Holder<T>) -> Unit) {
        register(registryKey) { _ ->
            registratorConsumer { id, value -> Registry.registerForHolder(getRegistry(registryKey)!!, id, value) }
        }
    }
}