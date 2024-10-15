package com.ruslan.growsseth

import com.filloax.fxlib.api.fabric.FabricReloadListener
import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.config.GrowssethConfigHandler
import com.ruslan.growsseth.dialogues.ResearcherDialogueApiListener
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.entity.researcher.ResearcherDiaryComponent
import com.ruslan.growsseth.entity.researcher.trades.GameMasterResearcherTradesProvider
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.networking.GrowssethPackets
import com.ruslan.growsseth.platform.PlatformAbstractions
import com.ruslan.growsseth.platform.platform
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.templates.TemplateListener
import com.ruslan.growsseth.utils.loadPropertiesFile
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
import java.util.*


object RuinsOfGrowssethFabric : ModInitializer, RuinsOfGrowsseth() {
    override fun onInitialize() {
       initialize()
    }

    override fun initRegistries() {
        GrowssethCreativeModeTabs.registerCreativeModeTabs { id, value -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, value) }
        GrowssethItems.registerItems{ id, value -> Registry.register(BuiltInRegistries.ITEM, id, value) }
        GrowssethItems.Instruments.registerInstruments{ id, value -> Registry.register(BuiltInRegistries.INSTRUMENT, id, value) }
        GrowssethItems.SherdPatterns.registerPotPatterns { id, value -> Registry.register(BuiltInRegistries.DECORATED_POT_PATTERN, id, value) }
        GrowssethMapDecorations.registerMapDecorations{ id, value -> Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE, id, value) }
        GrowssethEffects.registerEffects{ id, value -> Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, value) }
        GrowssethEntities.registerEntityTypes{ id, value -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, value) }
        GrowssethStructurePieceTypes.registerStructurePieces{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, value) }
        GrowssethStructures.registerStructureTypes{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, value) }
        GrowssethCriterions.registerCriterions { id, value -> Registry.register(BuiltInRegistries.TRIGGER_TYPES, id, value) }
        GrowssethCommands.ArgumentTypes.registerArgumentTypes(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)
        GrowssethPackets.registerPacketsC2S(platform.packetRegistratorC2S)
        GrowssethPackets.registerPacketsS2C(platform.packetRegistratorS2C)
    }

    override fun initItemGroups() {
        // TODO: make this abstracted in the common module
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
                it.addAfter(Items.GOAT_HORN, GrowssethItems.RESEARCHER_HORN)
                for (disc in GrowssethItems.DISCS_ORDERED) {
                    it.accept(disc)
                }
            })
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(ModifyEntries {
                it.addAfter(Items.DISC_FRAGMENT_5, GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
            })
    }

    override fun registerResourceListeners() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.TRADES_DATA_FOLDER),
            TradesListener(),
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.RESEARCHER_DIALOGUE_DATA_FOLDER),
            ResearcherDialogueListener(),
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.TEMPLATE_FOLDER),
            TemplateListener,
        ))
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(FabricReloadListener(
            resLoc(Constants.PRESET_PLACES_FOLDER),
            LocationNotifListener(),
        ))
    }
}