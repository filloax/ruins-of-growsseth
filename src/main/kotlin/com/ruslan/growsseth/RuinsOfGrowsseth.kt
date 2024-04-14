package com.ruslan.growsseth

import com.filloax.fxlib.fabric.FabricReloadListener
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.dialogues.BasicDialoguesComponent
import com.ruslan.growsseth.dialogues.ResearcherDialogueApiListener
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.*
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTrades
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.events.*
import com.ruslan.growsseth.http.*
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.quests.QuestComponentEvents
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.utils.AsyncLocator
import com.ruslan.growsseth.utils.MixinHelpers
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.*
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Items
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


object RuinsOfGrowsseth : ModInitializer {
    @JvmStatic
    val LOGGER: Logger = LogManager.getLogger()
    const val MOD_ID = "growsseth"
    const val MOD_NAME = "Ruins of Growsseth"

    override fun onInitialize() {
        log(Level.INFO, "Initializing")

        initEvents()

        GrowssethApi.current.init()
        GrowssethBannerPatterns.registerAll(BuiltInRegistries.BANNER_PATTERN)
        GrowssethCreativeModeTabs.registerCreativeModeTabs()
        GrowssethItems.registerAll(BuiltInRegistries.ITEM)
        GrowssethItems.Instruments.registerAll(BuiltInRegistries.INSTRUMENT)
        GrowssethEffects.registerAll(BuiltInRegistries.MOB_EFFECT)
        GrowssethEntities.init(BuiltInRegistries.ENTITY_TYPE)
        GrowssethStructurePieceTypes.registerAll(BuiltInRegistries.STRUCTURE_PIECE)
        GrowssethStructures.init(BuiltInRegistries.STRUCTURE_TYPE)
        RemoteStructures.init()
        CustomRemoteDiaries.init()
        RemoteStructureBooks.init()
        ResearcherTrades.init()
        GrowssethExtraEvents.init()
        ResearcherDialogueApiListener.init()
        GrowssethMapDecorations.init()

        initItemGroups()
        registerResourceListeners()

        GrowssethConfigHandler.initConfig()

        CommandRegistrationCallback.EVENT.register { d, ra, e -> GrowssethCommands.register(d, ra, e) }

        log(Level.INFO, "Initialized! :saidogPipo: :saidogRitto: :saidogMax:")
        log(Level.DEBUG, "In log debug mode!")
    }

    private fun initItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(ModifyEntries {
                // Piglin is last pattern
                it.addAfter(Items.PIGLIN_BANNER_PATTERN, GrowssethItems.GROWSSETH_BANNER_PATTERN)
                it.addAfter(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, GrowssethItems.GROWSSETH_ARMOR_TRIM)
                it.addAfter(Items.HEART_POTTERY_SHERD, GrowssethItems.GROWSSETH_POTTERY_SHERD)
            })
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(ModifyEntries {
                it.accept(GrowssethItems.RESEARCHER_SPAWN_EGG)
                it.accept(GrowssethItems.ZOMBIE_RESEARCHER_SPAWN_EGG)
            })
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
            .register(ModifyEntries {
                it.addAfter(Items.TRIDENT, GrowssethItems.RESEARCHER_DAGGER)
            })
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(ModifyEntries {
                it.addAfter(Items.MUSIC_DISC_RELIC, GrowssethItems.DISC_SEGA_DI_NIENTE)
                it.addAfter(GrowssethItems.DISC_SEGA_DI_NIENTE, GrowssethItems.DISC_GIORGIO_CUBETTI)
                it.addAfter(GrowssethItems.DISC_GIORGIO_CUBETTI, GrowssethItems.DISC_GIORGIO_LOFI)
                it.addAfter(GrowssethItems.DISC_GIORGIO_LOFI, GrowssethItems.DISC_GIORGIO_LOFI_INST)
                it.addAfter(GrowssethItems.DISC_GIORGIO_LOFI_INST, GrowssethItems.DISC_GIORGIO_FINDING_HOME)
                it.addAfter(GrowssethItems.DISC_GIORGIO_FINDING_HOME, GrowssethItems.DISC_BINOBINOOO)
                it.addAfter(GrowssethItems.DISC_BINOBINOOO, GrowssethItems.DISC_BINOBINOOO_INST)
                it.addAfter(GrowssethItems.DISC_BINOBINOOO_INST, GrowssethItems.DISC_PADRE_MAMMONK)
                it.addAfter(GrowssethItems.DISC_PADRE_MAMMONK, GrowssethItems.DISC_ABBANDONATI)
                it.addAfter(GrowssethItems.DISC_ABBANDONATI, GrowssethItems.DISC_MISSIVA_NELL_OMBRA)
                it.addAfter(GrowssethItems.DISC_MISSIVA_NELL_OMBRA, GrowssethItems.DISC_MICE_ON_VENUS)
                it.addAfter(GrowssethItems.DISC_MICE_ON_VENUS, GrowssethItems.DISC_INFINITE_AMETHYST)
                it.addAfter(GrowssethItems.DISC_INFINITE_AMETHYST, GrowssethItems.DISC_LABYRINTHINE)

                it.addAfter(Items.GOAT_HORN, GrowssethItems.RESEARCHER_HORN)
            })
    }

    private fun initEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            AsyncLocator.handleServerAboutToStartEvent()
            DataRemoteSync.handleServerAboutToStartEvent(server)
            Researcher.initServer(server)
            DataRemoteSync.doSync(WebConfig.dataSyncUrl, server)
            MixinHelpers.serverInit(server)
            LiveUpdatesConnection.serverStart(server)
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            GrowssethWorldPreset.Callbacks.onServerStarted(server)
            VillageBuildings.onServerStarted(server)
        }
        ServerWorldEvents.LOAD.register { server, level ->
            DataRemoteSync.handleWorldLoaded(server, level)
            ResearcherDiaryComponent.Callbacks.onServerLevel(level)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            AsyncLocator.handleServerStoppingEvent()
            DataRemoteSync.handleServerStoppingEvent()
            GrowssethApiV1.Callbacks.onServerStop(server)
            ResearcherTrades.onServerStop(server)
            LiveUpdatesConnection.serverStop(server)
            GrowssethExtraEvents.onServerStop()
            CustomRemoteDiaries.onServerStopped()
            RemoteStructureBooks.onServerStopped()
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            LocationNotifListener.Callbacks.onServerStopped(server)
        }
        ServerTickEvents.START_SERVER_TICK.register { server ->
            DataRemoteSync.checkTickSync(WebConfig.dataSyncUrl, server)
            GrowssethAdvancements.onServerTick(server)
        }

        ServerChunkEvents.CHUNK_LOAD.register { level, chunk ->
            GrowssethExtraEvents.Callbacks.onLoadChunk(level, chunk)
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, level ->
            QuestComponentEvents.onLoadEntity(entity)
        }
        ServerEntityEvents.ENTITY_UNLOAD.register { entity, level ->
//            QuestComponentEvents.onUnloadEntity(entity, level)
        }
        ServerEntityLifecycleEvents.ENTITY_DESTROYED.register { entity, level ->
            Researcher.Callbacks.onEntityDestroyed(entity, level)
        }

        PlayerBlockBreakEvents.AFTER.register { level, player, pos, state, entity ->
            ResearcherDialoguesComponent.Callbacks.onBlockBreak(level, player, pos, state, entity)
        }
        PlaceBlockEvent.AFTER.register { player, world, pos, placeContext, blockState, item ->
            ResearcherDialoguesComponent.Callbacks.onPlaceBlock(player, world, pos, placeContext, blockState, item)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            ResearcherTrades.onServerPlayerJoin(handler, sender, server)
            GrowssethExtraEvents.onServerPlayerJoin(handler, sender, server)
            GrowssethWorldPreset.Callbacks.onServerPlayerJoin(handler, sender, server)
        }

        PlayerAdvancementEvent.EVENT.register { player, advancement, criterionString ->
            BasicDialoguesComponent.Callbacks.onAdvancement(player, advancement, criterionString)
        }

        LeashEvents.FENCE_LEASH.register { mob, pos, player ->
            Researcher.Callbacks.onFenceLeash(mob, pos, player)
        }
        LeashEvents.FENCE_UNLEASH.register { mob, pos ->
            Researcher.Callbacks.onFenceUnleash(mob, pos)
        }

        // Register singularly because returns
        NameTagRenameEvent.BEFORE.register(Researcher.Callbacks::nameTagRename)
        //AttackEntityCallback.EVENT.register(BasicDialoguesComponent.Callbacks::onAttack)

        DisableStructuresEvents.STRUCTURE_GENERATE.register { level, structure, _, _, _, _, _, _, _, _ ->
            StructureDisabler.Callbacks.shouldDisableStructure(structure, level)
        }
    }

    private fun registerResourceListeners() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.TRADES_DATA_FOLDER),
            TradesListener(),
        )
        )
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.RESEARCHER_DIALOGUE_DATA_FOLDER),
            ResearcherDialogueListener(),
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.RESEARCHER_DIARY_DATA_FOLDER),
            DiaryListener(),
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.STRUCTURE_BOOK_FOLDER),
            StructureBookListener(),
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.PRESET_PLACES_FOLDER),
            LocationNotifListener(),
        ))
    }

    fun log(level: Level, message: String) {
        LOGGER.log(level, "[$MOD_NAME] $message")
    }

    fun logDev(level: Level, message: String) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            LOGGER.log(level, "[$MOD_NAME] $message")
        }
    }
}