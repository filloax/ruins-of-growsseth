package com.ruslan.growsseth

import com.filloax.fxlib.api.fabric.FabricReloadListener
import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.dialogues.ResearcherDialogueApiListener
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.DiaryListener
import com.ruslan.growsseth.entity.researcher.trades.GameMasterResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.networking.GrowssethPackets
import com.ruslan.growsseth.platform.PlatformAbstractions
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Items
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


object RuinsOfGrowsseth : ModInitializer {
    @JvmStatic
    val LOGGER: Logger = LogManager.getLogger()
    val platform = PlatformAbstractions.get()
    const val MOD_ID = "growsseth"
    const val MOD_NAME = "Ruins of Growsseth"

    override fun onInitialize() {
        log(Level.INFO, "Initializing")

        ModEvents.get().initCallbacks()

        GrowssethApi.current.init()
        initRegistries()
        RemoteStructures.init()
        CustomRemoteDiaries.init()
        RemoteStructureBooks.init()
        GameMasterResearcherTradesProvider.subscribe()
        GrowssethExtraEvents.init()
        ResearcherDialogueApiListener.init()

        initItemGroups()
        registerResourceListeners()

        GrowssethConfigHandler.initConfig()

        CommandRegistrationCallback.EVENT.register { d, ra, e -> GrowssethCommands.register(d, ra, e) }

        log(Level.INFO, "Initialized! :saidogPipo: :saidogRitto: :saidogMax:")
        log(Level.DEBUG, "In log debug mode!")
    }

    private fun initRegistries() {
        GrowssethCreativeModeTabs.registerCreativeModeTabs()
        GrowssethItems.registerItems{ id, value -> Registry.register(BuiltInRegistries.ITEM, id, value) }
        GrowssethItems.Instruments.registerInstruments{ id, value -> Registry.register(BuiltInRegistries.INSTRUMENT, id, value) }
        GrowssethMapDecorations.registerMapDecorations{ id, value -> Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE, id, value) }
        GrowssethEffects.registerEffects{ id, value -> Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, value) }
        GrowssethEntities.registerEntityTypes{ id, value -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, value) }
        GrowssethStructurePieceTypes.registerStructurePieces{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, value) }
        GrowssethStructures.registerStructureTypes{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, value) }
        GrowssethCriterions.registerCriterions { id, value -> Registry.register(BuiltInRegistries.TRIGGER_TYPES, id, value) }
        GrowssethCommands.ArgumentTypes.registerArgumentTypes(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)
        GrowssethPackets.registerPacketsC2S(platform)
        GrowssethPackets.registerPacketsS2C(platform)
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
                it.addAfter(Items.MUSIC_DISC_PIGSTEP, GrowssethItems.DISC_SEGA_DI_NIENTE)
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