package com.ruslan.growsseth

import com.ruslan.growsseth.RuinsOfGrowssethNeo.register
import com.ruslan.growsseth.RuinsOfGrowssethNeo.registerHolder
import com.ruslan.growsseth.advancements.GrowssethCriterions
import com.ruslan.growsseth.client.GrowssethClientNeo
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
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(RuinsOfGrowsseth.MOD_ID)
object RuinsOfGrowssethNeo : RuinsOfGrowsseth() {
    init {
        initialize()

        runForDist(
            clientTarget = {
                MOD_BUS.addListener(GrowssethClientNeo::initializeClient)
                Minecraft.getInstance()
            },
            serverTarget = {}
        )
    }

    override fun initItemGroups() {
        MOD_BUS.addListener { ev: BuildCreativeModeTabContentsEvent ->
            val entries = ev.searchEntries.associateBy { it.item }
            val addAfter = { item: Item, new: Item ->
                ev.insertAfter(entries[item]!!, new.defaultInstance, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)
            }

            when (ev.tab) {
                CreativeModeTabs.INGREDIENTS -> {
                    addAfter(Items.PIGLIN_BANNER_PATTERN, GrowssethItems.GROWSSETH_BANNER_PATTERN)
                    addAfter(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, GrowssethItems.GROWSSETH_ARMOR_TRIM)
                    addAfter(Items.HEART_POTTERY_SHERD, GrowssethItems.GROWSSETH_POTTERY_SHERD)
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
                CreativeModeTabs.INGREDIENTS -> {
                    addAfter(Items.DISC_FRAGMENT_5, GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
                }
            }
        }
    }

    override fun registerResourceListeners() {
        FORGE_BUS.addListener { ev: AddReloadListenerEvent ->
            ev.addListener(TradesListener())
            ev.addListener(ResearcherDialogueListener())
            ev.addListener(TemplateListener)
            ev.addListener(LocationNotifListener())
        }
    }

    override fun initRegistries() {
        MOD_BUS.addListener { ev: RegisterEvent ->
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
            // GrowssethCommands.ArgumentTypes.registerArgumentTypes(Registries.COMMAND_ARGUMENT_TYPE)
        }
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