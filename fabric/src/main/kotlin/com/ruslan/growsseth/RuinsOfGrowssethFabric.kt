package com.ruslan.growsseth

import com.filloax.fxlib.api.fabric.FabricReloadListener
import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.compat.cobblemon.CobblemonRCTListener
import com.ruslan.growsseth.dialogues.ResearcherDialogueListener
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.trades.TradesListener
import com.ruslan.growsseth.item.GrowssethCreativeModeTabs
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.maps.GrowssethMapDecorations
import com.ruslan.growsseth.sound.GrowssethSounds
import com.ruslan.growsseth.structure.*
import com.ruslan.growsseth.templates.TemplateListener
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.worldpreset.LocationNotifListener
import com.teamremastered.endrem.registry.ERTabs
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.ModifyOutput
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Items


object RuinsOfGrowssethFabric : ModInitializer, RuinsOfGrowsseth() {
    override fun onInitialize() {
       initialize()
    }

    override fun initRegistries() {
        GrowssethSounds.registerSoundEvents { id, value -> Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, value) }
        GrowssethCreativeModeTabs.registerCreativeModeTabs { id, value -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, value) }
        //GrowssethItems.Instruments.registerInstruments{ id, value -> Registry.register(BuiltInRegistries.INSTRUMENT, id, value) }
        GrowssethItems.SherdPatterns.registerPotPatterns { id, value -> Registry.register(BuiltInRegistries.DECORATED_POT_PATTERN, id, value) }
        GrowssethMapDecorations.registerMapDecorations{ id, value -> Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE, id, value) }
        GrowssethEffects.registerEffects{ id, value -> Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, value) }
        GrowssethEntities.registerEntityTypes{ id, value -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, value) }
        GrowssethItems.registerItems{ id, value -> Registry.register(BuiltInRegistries.ITEM, id, value) }
        GrowssethStructurePieceTypes.registerStructurePieces{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id, value) }
        GrowssethStructures.registerStructureTypes{ id, value -> Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, value) }
        GrowssethCriterions.registerCriterions { id, value -> Registry.register(BuiltInRegistries.TRIGGER_TYPES, id, value) }
        GrowssethCommands.ArgumentTypes.registerArgumentTypes(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)

        CommandRegistrationCallback.EVENT.register { d, ra, e -> GrowssethCommands.register(d, ra, e) }
    }

    override fun initItemGroups() {
        // More convenient to do this per-loader than AT all the creativeModeTabs entries (which are private by default)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(ModifyOutput {
                // Piglin is last pattern
                it.insertAfter(Items.PIGLIN_BANNER_PATTERN, GrowssethItems.GROWSSETH_BANNER_PATTERN)
                it.insertAfter(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, GrowssethItems.GROWSSETH_ARMOR_TRIM)
                it.insertAfter(Items.HEART_POTTERY_SHERD, GrowssethItems.GROWSSETH_POTTERY_SHERD)
                it.insertAfter(Items.DISC_FRAGMENT_5, GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
            })
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(ModifyOutput {
                it.accept(GrowssethItems.RESEARCHER_SPAWN_EGG)
                it.accept(GrowssethItems.ZOMBIE_RESEARCHER_SPAWN_EGG)
            })
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
            .register(ModifyOutput {
                it.insertAfter(Items.TRIDENT, GrowssethItems.RESEARCHER_DAGGER)
            })
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(ModifyOutput {
                it.insertAfter(Items.GOAT_HORN, GrowssethItems.RESEARCHER_HORN)
                for (disc in GrowssethItems.DISCS_ORDERED) {
                    it.accept(disc)
                }
            })

        if (modCompat.isEndRemasteredLoaded) {
            ItemGroupEvents.modifyEntriesEvent(ERTabs.ITEM_GROUP)
                .register { it.accept(GrowssethItems.ENDREM_GROWSSETH_EYE) }
        }
    }

    override fun registerResourceListeners() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(resLoc("trades_reloader"),
            FabricReloadListener(
                resLoc(Constants.TRADES_DATA_FOLDER),
                TradesListener(),
            )
        )
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(resLoc("dialogues_reloader"),
            FabricReloadListener(
                resLoc(Constants.RESEARCHER_DIALOGUE_DATA_FOLDER),
                ResearcherDialogueListener(),
            )
        )
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(resLoc("templates_reloader"),
            FabricReloadListener(
                resLoc(Constants.TEMPLATE_FOLDER),
                TemplateListener,
            )
        )
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(resLoc("preset_places_reloader"),
            FabricReloadListener(
                resLoc(Constants.PRESET_PLACES_FOLDER),
                LocationNotifListener(),
            )
        )
        if (modCompat.isAllCobblemonDepsLoaded) {
            ResourceLoader.get(PackType.SERVER_DATA).registerReloader(resLoc("cobblemon_compat_reloader"),
                FabricReloadListener(
                    resLoc(Constants.COMPAT_COBBLEMON_FOLDER),
                    CobblemonRCTListener,
                )
            )
        }
    }
}