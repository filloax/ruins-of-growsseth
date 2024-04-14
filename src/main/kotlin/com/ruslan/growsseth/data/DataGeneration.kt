package com.ruslan.growsseth.data

import com.ruslan.growsseth.GrowssethLootTables
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.StructureAdvancements
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.researcher.ZombieResearcher
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.worldgen.GrowssethModWorldPresets
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.ItemTagProvider
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.loot.LootTableSubProvider
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.data.models.ItemModelGenerators
import net.minecraft.data.models.model.ModelTemplates
import net.minecraft.data.tags.InstrumentTagsProvider
import net.minecraft.data.tags.WorldPresetTagsProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.WorldPresetTags
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Consumer


// BIG thanks to https://github.com/Ayutac/fabric-example-worldgen
// fixed my headache
class DataGeneration : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

        pack.addProvider(::RegistriesProvider)
        pack.addProvider(::AdvancementsProvider)
        pack.addProvider(::TagProviderBlocks)
        pack.addProvider(::TagProviderItems)
        pack.addProvider(::TagProviderInstruments)
        pack.addProvider(::TagProviderWorldPresets)
        pack.addProvider(::EntityLootTableProvider)
        pack.addProvider(::MiscLootTableProvider)
        pack.addProvider(::ModelGenerator)
    }

    override fun buildRegistry(registryBuilder: RegistrySetBuilder) {
        registryBuilder.add(Registries.STRUCTURE, GrowssethStructures::bootstrap)
        registryBuilder.add(Registries.WORLD_PRESET, GrowssethModWorldPresets::bootstrap)
        registryBuilder.add(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, GrowssethWorldPreset::bootstrapNoiseBiomeSourcesSettings)
    }

    override fun getEffectiveModId(): String = RuinsOfGrowsseth.MOD_ID
}

class RegistriesProvider(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>) : FabricDynamicRegistryProvider(output, registries) {
    override fun getName(): String = "Growsseth World Gen"

    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE))
        entries.addAll(registries.lookupOrThrow(Registries.WORLD_PRESET))
        entries.addAll(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST))
    }
}


class AdvancementsProvider(output: FabricDataOutput) :
    FabricAdvancementProvider(output) {
    override fun generateAdvancement(consumer: Consumer<AdvancementHolder>) {
        StructureAdvancements.generateForStructureDetection(consumer)
        //DonkeyAdvancements.generateDonkeyAdvancements(consumer) //not needed
    }
}

class TagProviderInstruments(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>): InstrumentTagsProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateRawBuilder(GrowssethTags.RESEARCHER_HORNS)
            .addOptionalElement(GrowssethItems.Instruments.RESEARCHER_HORN.first)
    }
}

class TagProviderBlocks(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): BlockTagProvider(output, registries) {
    /**
     * Implement this method and then use [FabricTagProvider.tag] to get and register new tag builders.
     */
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateTagBuilder(GrowssethTags.TENT_MATERIALS_WHITELIST)
            .addOptionalTag(ResourceLocation("planks"))
            .addOptionalTag(ResourceLocation("wool"))
            .addOptionalTag(ResourceLocation("wooden_stairs"))
            .addOptionalTag(ResourceLocation("wooden_slabs"))
            .addOptionalTag(ResourceLocation("wooden_pressure_plates"))
            .addOptionalTag(ResourceLocation("trapdoors"))
            .addOptionalTag(ResourceLocation("banners"))
            .addOptionalTag(ResourceLocation("fences"))
            .addOptionalTag(ResourceLocation("walls"))
            .addOptionalTag(ResourceLocation("c", "chests"))
            .addOptionalTag(ResourceLocation("wool_carpets"))
            .addOptionalTag(ResourceLocation("campfires"))
            .addOptionalTag(ResourceLocation("c", "villager_job_sites"))
            .addOptionalTag(ResourceLocation("fence_gates"))
            .addOptionalTag(ResourceLocation("logs"))
            .add(Blocks.LECTERN)
            .add(Blocks.CARTOGRAPHY_TABLE)
            .add(Blocks.IRON_BARS)
            .add(Blocks.IRON_BARS)
            .add(Blocks.LADDER)
            .add(Blocks.LANTERN)
            .add(Blocks.TORCH)
            .add(Blocks.CHAIN)
        getOrCreateTagBuilder(GrowssethTags.TENT_CLEAR_ZOMBIE_STAGE_WHITELIST)
            .add(Blocks.WHITE_CARPET)
        getOrCreateTagBuilder(GrowssethTags.RESEARCHER_MESS_TRIGGER)
            .add(Blocks.LECTERN)
            .add(Blocks.CARTOGRAPHY_TABLE)
    }
}

class TagProviderItems(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): ItemTagProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateTagBuilder(ItemTags.DECORATED_POT_SHERDS)
            .add(GrowssethItems.GROWSSETH_POTTERY_SHERD)
    }
}

class TagProviderWorldPresets(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): WorldPresetTagsProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateRawBuilder(WorldPresetTags.NORMAL)
            .addElement(GrowssethModWorldPresets.GROWSSETH.location())
    }
}

class EntityLootTableProvider(output: FabricDataOutput) : SimpleFabricLootTableProvider(output, LootContextParamSets.ENTITY) {
    override fun generate(consumer: BiConsumer<ResourceLocation, LootTable.Builder>) {
        consumer.accept(GrowssethEntities.ZOMBIE_RESEARCHER.defaultLootTable, ZombieResearcher.getLootTable())
    }

    /**
     * Gets a name for this provider, to use in logging.
     */
    override fun getName(): String = "GrowssethEntityLootTable"
}

class MiscLootTableProvider(output: PackOutput): LootTableProvider(output, setOf(), mutableListOf(
    SubProviderEntry({
        LootTableSubProvider { builder ->
            builder.accept(
                GrowssethLootTables.CONDUIT_RUINS_ARCHAEOLOGY,
                LootTable.lootTable()
                    .withPool(
                        LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(GrowssethItems.GROWSSETH_POTTERY_SHERD))
                    )
            )
        }
    }, LootContextParamSets.ARCHAEOLOGY)
))


class ModelGenerator constructor(generator: FabricDataOutput) : FabricModelProvider(generator) {
    override fun generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators?) {
    }

    override fun generateItemModels(itemModelGenerator: ItemModelGenerators?) {
        GrowssethItems.all.forEach { (key, item) ->
            if (item !in GrowssethItems.noAutogenerateItems) {
                itemModelGenerator!!.generateFlatItem(item, ModelTemplates.FLAT_ITEM)
            }
        }
    }
}