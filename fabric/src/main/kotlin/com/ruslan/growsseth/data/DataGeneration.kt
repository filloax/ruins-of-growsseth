package com.ruslan.growsseth.data

import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.item.GrowssethJukeboxSongs
import com.ruslan.growsseth.structure.GrProcessorLists
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.worldgen.GrowssethModWorldPresets
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.*
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.ItemTagProvider
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.model.ModelLocationUtils
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.packs.VanillaRecipeProvider.TrimTemplate
import net.minecraft.data.tags.BannerPatternTagsProvider
import net.minecraft.data.tags.StructureTagsProvider
import net.minecraft.data.tags.WorldPresetTagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.WorldPresetTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

// Datagen in fabric, arbitrary whether to do it here or in neforge
// BIG thanks to https://github.com/Ayutac/fabric-example-worldgen
// fixed my headache
class DataGeneration : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

        pack.addProvider(::RegistriesProvider)
        pack.addProvider(::RecipesProvider)
        pack.addProvider(::TagProviderBlocks)
        pack.addProvider(::TagProviderItems)
        //pack.addProvider(::TagProviderInstruments)
        pack.addProvider(::TagProviderStructures)
        pack.addProvider(::TagProviderWorldPresets)
        pack.addProvider(::TagProviderBannerPatterns)
        pack.addProvider(::AdvancementsProvider)
        //pack.addProvider(::EntityLootTableProvider)
        //pack.addProvider(::MiscLootTableProvider)
        pack.addProvider(::ModelGenerator)
        pack.addProvider(::CustomDataProvider)

    }

    override fun buildRegistry(registryBuilder: RegistrySetBuilder) {
        registryBuilder.add(Registries.PROCESSOR_LIST, GrProcessorLists::bootstrap)
        registryBuilder.add(Registries.TEMPLATE_POOL, SimplePools.Companion::bootstrap)
        registryBuilder.add(Registries.STRUCTURE, GrowssethStructures::bootstrap)
        registryBuilder.add(Registries.WORLD_PRESET, GrowssethModWorldPresets::bootstrap)
        registryBuilder.add(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, GrowssethWorldPreset::bootstrapNoiseBiomeSourcesSettings)
        registryBuilder.add(Registries.BANNER_PATTERN, com.ruslan.growsseth.GrowssethBannerPatterns::bootstrap)
        registryBuilder.add(Registries.JUKEBOX_SONG, GrowssethJukeboxSongs::bootstrap)
    }

    override fun getEffectiveModId(): String = RuinsOfGrowsseth.MOD_ID
}

class RegistriesProvider(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>) : FabricDynamicRegistryProvider(output, registries) {
    override fun getName(): String = "Growsseth Registries"

    override fun configure(registries: HolderLookup.Provider, entries: Entries) {
        entries.addAll(registries.lookupOrThrow(Registries.PROCESSOR_LIST))
        entries.addAll(registries.lookupOrThrow(Registries.TEMPLATE_POOL))
        entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE))
        entries.addAll(registries.lookupOrThrow(Registries.WORLD_PRESET))
        entries.addAll(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST))
        entries.addAll(registries.lookupOrThrow(Registries.BANNER_PATTERN))
        entries.addAll(registries.lookupOrThrow(Registries.JUKEBOX_SONG))
    }
}

class RecipesProvider(output: FabricDataOutput, registriesFuture: CompletableFuture<HolderLookup.Provider>)
    : FabricRecipeProvider(output, registriesFuture) {

    protected override fun createRecipeProvider(
        registryLookup: HolderLookup.Provider,
        exporter: RecipeOutput
    ): RecipeProvider {
        return object: RecipeProvider(registryLookup, exporter) {

            override fun buildRecipes() {
                this.copySmithingTemplate(GrowssethItems.GROWSSETH_ARMOR_TRIM, Items.COBBLED_DEEPSLATE)
                listOf(
                    GrowssethItems.GROWSSETH_ARMOR_TRIM,
                ).forEach {
                    val trimTemplate = TrimTemplate(
                        it,
                        ResourceKey.create(
                            Registries.TRIM_PATTERN, ResourceLocation.parse(getItemName(it) + "_smithing_trim")
                        ),
                        ResourceKey.create(
                            Registries.RECIPE, ResourceLocation.parse(getItemName(it) + "_smithing_trim")
                        )
                    )
                    this.trimSmithing(trimTemplate.template, trimTemplate.patternId, trimTemplate.recipeId)
                }

                GrowssethItems.DISCS_TO_VOCALS.forEach { vocalsDiscRecipe(exporter, it.key, it.value) }

                GrowssethItems.FRAGMENTS_TO_DISCS.forEach { fragmentToDiscRecipe(exporter, it.key, it.value) }
            }

            private fun vocalsDiscRecipe(exporter: RecipeOutput, baseDisc: ItemLike, vocalsDisc: ItemLike) {
                val items = registries.lookupOrThrow(Registries.ITEM)
                ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, vocalsDisc)
                    .requires(Items.AMETHYST_SHARD)
                    .requires(baseDisc)
                    .unlockedBy(RecipeProvider.getHasName(baseDisc), this.has(baseDisc))
                    .save(exporter)
            }

            private fun fragmentToDiscRecipe (exporter: RecipeOutput, discFragment: ItemLike, disc: ItemLike) {
                val items = registries.lookupOrThrow(Registries.ITEM)
                ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, disc)
                    .requires(discFragment, 9)
                    .unlockedBy(RecipeProvider.getHasName(discFragment), this.has(discFragment))
                    .save(exporter)
            }
        }
    }

    override fun getName(): String {
        return "GrowssethRecipeProvider"
    }
}

class AdvancementsProvider(output: FabricDataOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) :
    FabricAdvancementProvider(output, registryLookup) {
    override fun generateAdvancement(registryLookup: HolderLookup.Provider, consumer: Consumer<AdvancementHolder>) {
        StructureAdvancements.Bootstrapper(registryLookup).generateForStructureDetection(consumer)
        //DonkeyAdvancements.generateDonkeyAdvancements(consumer) //not needed
    }
}

//class TagProviderInstruments(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>): InstrumentTagsProvider(output, registries) {
//    override fun addTags(arg: HolderLookup.Provider) {
//        getOrCreateRawBuilder(GrowssethTags.RESEARCHER_HORNS)
//            .addOptionalElement(GrowssethItems.Instruments.RESEARCHER_HORN.first)
//    }
//}

class TagProviderBlocks(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): BlockTagProvider(output, registries) {
    /**
     * Implement this method and then use [FabricTagProvider.tag] to get and register new tag builders.
     */
    override fun addTags(arg: HolderLookup.Provider) {
        valueLookupBuilder(GrowssethTags.TENT_MATERIALS_WHITELIST)
            .addOptionalTag(BlockTags.PLANKS)
            .addOptionalTag(BlockTags.WOOL)
            .addOptionalTag(BlockTags.WOODEN_STAIRS)
            .addOptionalTag(BlockTags.WOODEN_SLABS)
            .addOptionalTag(BlockTags.WOODEN_PRESSURE_PLATES)
            .addOptionalTag(BlockTags.TRAPDOORS)
            .addOptionalTag(BlockTags.BANNERS)
            .addOptionalTag(BlockTags.FENCES)
            .addOptionalTag(BlockTags.WALLS)
            .addOptionalTag(ConventionalBlockTags.CHESTS)
            .addOptionalTag(BlockTags.WOOL_CARPETS)
            .addOptionalTag(BlockTags.CAMPFIRES)
            .addOptionalTag(ConventionalBlockTags.VILLAGER_JOB_SITES)
            .addOptionalTag(BlockTags.FENCE_GATES)
            .add(Blocks.STRIPPED_SPRUCE_LOG)
            .add(Blocks.LECTERN)
            .add(Blocks.CARTOGRAPHY_TABLE)
            .add(Blocks.IRON_BARS)
            .add(Blocks.IRON_BARS)
            .add(Blocks.LADDER)
            .add(Blocks.LANTERN)
            .add(Blocks.TORCH)
            .add(Blocks.IRON_CHAIN)
            .add(Blocks.HAY_BLOCK)
        valueLookupBuilder(GrowssethTags.TENT_CLEAR_ZOMBIE_STAGE_WHITELIST)
            .add(Blocks.WHITE_CARPET)
        valueLookupBuilder(GrowssethTags.RESEARCHER_MESS_TRIGGER)
            .add(Blocks.LECTERN)
            .add(Blocks.CARTOGRAPHY_TABLE)
    }
}

class TagProviderItems(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): ItemTagProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        valueLookupBuilder(ItemTags.DECORATED_POT_SHERDS)
            .add(GrowssethItems.GROWSSETH_POTTERY_SHERD)
    }
}

class TagProviderStructures(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): StructureTagsProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        GrowssethStructures.info.values.groupBy { it.tag }.forEach { (tag, infos) ->
            tag(tag).also { b ->
//                infos.forEach { b.addOptional(it.key.location()) }
                // uglier version in output: use optionals for non-datagenned structures thus not available here
                infos.forEach { (key, _) ->
                    if (arg.lookupOrThrow(Registries.STRUCTURE).get(key).isPresent) {
                        b.add(key)
                    } else {
                        b.addOptional(key)
                    }
                }
            }
        }
    }
}

class TagProviderWorldPresets(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): WorldPresetTagsProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateRawBuilder(WorldPresetTags.NORMAL)
            .addElement(GrowssethModWorldPresets.GROWSSETH.location())
    }
}

class TagProviderBannerPatterns(output: FabricDataOutput, registries: CompletableFuture<HolderLookup.Provider>): BannerPatternTagsProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        com.ruslan.growsseth.GrowssethBannerPatterns.all.forEach { banner ->
            getOrCreateRawBuilder(banner.tag)
                .addElement(banner.id().location())
        }
    }
}

/* // Not needed for now, since it's set in the zombie's class (might be used in the future to allow loot customization)
class EntityLootTableProvider(output: FabricDataOutput) : SimpleFabricLootTableProvider(output, LootContextParamSets.ENTITY) {
    override fun generate(consumer: BiConsumer<ResourceLocation, LootTable.Builder>) {
        consumer.accept(GrowssethEntities.ZOMBIE_RESEARCHER.defaultLootTable, ZombieResearcher.getLootTable())
    }

    /**
     * Gets a name for this provider, to use in logging.
     */
    override fun getName(): String = "GrowssethEntityLootTable"
}
*/

/* // Put aside for now to use manual method, might be used in the future
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
*/

// todo: add researcher horn to datagen
//// For some RegistrySetBuilder builder
//builder.add(Registries.INSTRUMENT, bootstrap -> {
//    bootstrap.register(
//        ResourceKey.create(Registries.INSTRUMENT, ResourceLocation.fromNamespaceAndPath("examplemod", "example_instrument")),
//        new Instrument(
//                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARROW_HIT),
//        7f,
//        256f,
//        Component.translatable(Util.makeDescriptionId("instrument", ResourceLocation.fromNamespaceAndPath("examplemod", "example_instrument")))
//    )
//    )
//});

class ModelGenerator constructor(generator: FabricDataOutput) : FabricModelProvider(generator) {
    override fun generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators?) {
    }

    override fun generateItemModels(itemModelGenerator: ItemModelGenerators) {
        GrowssethItems.all.forEach { (key, item) ->
            if (item !in GrowssethItems.noAutogenerateItems) {
                val model = ModelLocationUtils.getModelLocation(item)
                val discsBaseLayer = resLoc("item/discs_base")
                val discsVocalsLayer = resLoc("item/discs_vocals_glare")
                val discsSongLayer = resLoc("item/music_discs/${key.path}")
                when (item) {       // if a disc can be crafted (or is Oursteps) it will get the glare
                    GrowssethItems.DISC_OURSTEPS ->
                        itemModelGenerator.generateLayeredItem(model, ResourceLocation.parse("item/music_disc_pigstep"), discsVocalsLayer)
                    in GrowssethItems.DISCS_TO_VOCALS.values ->
                        itemModelGenerator.generateLayeredItem(model, discsBaseLayer, discsVocalsLayer, discsSongLayer)
                    in GrowssethItems.DISCS_ORDERED ->
                        itemModelGenerator.generateLayeredItem(model, discsBaseLayer, discsSongLayer)
                    else ->
                        itemModelGenerator.generateFlatItem(item, ModelTemplates.FLAT_ITEM)
                }
            }
        }
    }
}