package com.ruslan.growsseth

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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import java.util.function.Supplier

@Mod(RuinsOfGrowsseth.MOD_ID)
object RuinsOfGrowssethNeo : RuinsOfGrowsseth() {
    //region registries
    private val registries = mutableListOf<DeferredRegister<*>>()

    private val CREATIVE_MODE_TAB = createReg(BuiltInRegistries.CREATIVE_MODE_TAB)
    private val ITEM = createReg(BuiltInRegistries.ITEM)
    private val INSTRUMENT = createReg(BuiltInRegistries.INSTRUMENT)
    private val DECORATED_POT_PATTERN = createReg(BuiltInRegistries.DECORATED_POT_PATTERN)
    private val MAP_DECORATION_TYPE = createReg(BuiltInRegistries.MAP_DECORATION_TYPE)
    private val MOB_EFFECT = createReg(BuiltInRegistries.MOB_EFFECT)
    private val ENTITY_TYPE = createReg(BuiltInRegistries.ENTITY_TYPE)
    private val STRUCTURE_PIECE = createReg(BuiltInRegistries.STRUCTURE_PIECE)
    private val STRUCTURE_TYPE = createReg(BuiltInRegistries.STRUCTURE_TYPE)
    private val TRIGGER_TYPES = createReg(BuiltInRegistries.TRIGGER_TYPES)
    private val COMMAND_ARGUMENT_TYPE = createReg(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)
    //endregion registries

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
        MOD_BUS.register { ev: BuildCreativeModeTabContentsEvent ->
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
        MOD_BUS.register { ev: AddReloadListenerEvent ->
            ev.addListener(TradesListener())
            ev.addListener(ResearcherDialogueListener())
            ev.addListener(TemplateListener)
            ev.addListener(LocationNotifListener())
        }
    }

    override fun initRegistries() {
        GrowssethCreativeModeTabs.registerCreativeModeTabs(CREATIVE_MODE_TAB::doRegister)
        GrowssethItems.registerItems(ITEM::doRegister)
        GrowssethItems.Instruments.registerInstruments(INSTRUMENT::doRegister)
        GrowssethItems.SherdPatterns.registerPotPatterns(DECORATED_POT_PATTERN::doRegister)
        GrowssethMapDecorations.registerMapDecorations(MAP_DECORATION_TYPE::doRegister)
        GrowssethEffects.registerEffects(MOB_EFFECT::doRegister)
        GrowssethEntities.registerEntityTypes(ENTITY_TYPE::doRegister)
        GrowssethStructurePieceTypes.registerStructurePieces(STRUCTURE_PIECE::doRegister)
        GrowssethStructures.registerStructureTypes(STRUCTURE_TYPE::doRegister)
        GrowssethCriterions.registerCriterions(TRIGGER_TYPES::doRegister)
        // TODO
//        GrowssethCommands.ArgumentTypes.registerArgumentTypes(BuiltInRegistries.COMMAND_ARGUMENT_TYPE)

        registerRegistries(MOD_BUS)
    }



    private fun <T> createReg(builtin: Registry<T>): DeferredRegister<T> {
        return DeferredRegister.create(builtin, MOD_ID).also(registries::add)
    }
    private fun registerRegistries(bus: IEventBus) = registries.forEach { it.register(bus) }
}
fun <T> DeferredRegister<T>.doRegister(name: ResourceLocation, value: T): Holder<T> {
    return register(name.path, Supplier { value })
}