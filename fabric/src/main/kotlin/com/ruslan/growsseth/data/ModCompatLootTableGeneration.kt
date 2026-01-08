package com.ruslan.growsseth.data

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.github.yajatkaul.mega_showdown.item.MegaShowdownItems
import com.ruslan.growsseth.GrowssethLootTables
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.compat.data.OptionalLootItemTags
import com.ruslan.growsseth.item.GrowssethItems
import dev.architectury.registry.registries.RegistrySupplier
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootPool.lootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.LootTable.lootTable
import net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem
import net.minecraft.world.level.storage.loot.entries.TagEntry.expandTag
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer


class ModCompatMiscLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.EMPTY) {

    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {
        output.accept(
            GrowssethLootTables.COBBLEMON_DEFEAT_RESEARCHER,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.ENCHANTED_GOLDEN_APPLE))
                    .add(lootTableItem(Items.NETHERITE_SCRAP).count(between(1f, 4f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
                        .count(between(0f, 2f))
                    )
                )
        )
    }

    override fun getName(): String = "GrowssethModCompatMiscLootTable"
}

class ModCompatStructureLootTableGeneration(val registries: CompletableFuture<HolderLookup.Provider>, val optionalLootItemTags: OptionalLootItemTags) {
    companion object {
        private const val CHANCE_COMMON = 4f/5
        private const val CHANCE_RARE = 1f/2
        private const val CHANCE_RARER = 1f/3
        private const val CHANCE_RAREST = 1f/10

        // avoid depending on too many mods
        private val BOTTLE_CAP_SILVER = ResourceLocation.fromNamespaceAndPath("obc", "bottle_cap")
        private val BOTTLE_CAP_GOLD = ResourceLocation.fromNamespaceAndPath("obc", "bottle_cap_gold")
        private val TMCRAFT_BLANK_DISC_COPPER = ResourceLocation.fromNamespaceAndPath("tmcraft", "copper_blank_disc")
        private val TMCRAFT_BLANK_DISC_IRON = ResourceLocation.fromNamespaceAndPath("tmcraft", "iron_blank_disc")
        private val TMCRAFT_BLANK_DISC_GOLD = ResourceLocation.fromNamespaceAndPath("tmcraft", "gold_blank_disc")
        private val TMCRAFT_BLANK_DISC_DIAMOND = ResourceLocation.fromNamespaceAndPath("tmcraft", "diamond_blank_disc")
        private val TMCRAFT_BLANK_DISC_EMERALD = ResourceLocation.fromNamespaceAndPath("tmcraft", "emerald_blank_disc")
        private val TMCRAFT_BLANK_DISC_NETHERITE = ResourceLocation.fromNamespaceAndPath("tmcraft", "netherite_blank_disc")
        private val SIMPLETMS_BLANK_TR = ResourceLocation.fromNamespaceAndPath("simpletms", "tr_blank")
        private val SIMPLETMS_BLANK_TM = ResourceLocation.fromNamespaceAndPath("simpletms", "tm_blank")
        private val COBBLECUISINE_MILD_HONEY_CURRY = ResourceLocation.fromNamespaceAndPath("cobblecuisine", "mild_honey_curry")
        private val TMCRAFT_MOVE_UPGRADE_TEMPLATE = ResourceLocation.fromNamespaceAndPath("tmcraft", "move_upgrade_smithing_template")
    }

    fun generateForgeLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.HEAVY_BALL, 5, 15).common())
            .withPool(lootPool()
                .setRolls(between(2f, 6f))
                .add(optLootItem(CobblemonItems.BLUE_APRICORN).count(between(2f, 4f)))
                .add(optLootItem(CobblemonItems.BLACK_APRICORN).count(between(2f, 4f)))
                .common()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.METAL_COAT))
                .add(optLootItem(CobblemonItems.BLACK_AUGURITE))
                .add(optLootItem(CobblemonItems.PROTECTOR))
                .add(optLootItem(CobblemonItems.AUSPICIOUS_ARMOR))
                .add(optLootItem(CobblemonItems.MALICIOUS_ARMOR))
                .add(optLootItem(CobblemonItems.SHELL_HELMET))
                .add(optLootItem(CobblemonItems.METAL_ALLOY))
                .rare()
            )
            .withPool(singleItemPool(CobblemonItems.STEEL_GEM, 1, 2).rare())
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).common())
            .withPool(singleItemPool(BOTTLE_CAP_GOLD, 1, 1).rare())
            .withPool(singleItemPool(TMCRAFT_MOVE_UPGRADE_TEMPLATE, 1, 1).common())
            .build()
    }

    fun generateForgeSecretLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_IRON, 2, 4).common())
            .withPool(singleItemPool(SIMPLETMS_BLANK_TR, 2, 4).common())
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_GOLD, 1, 2).common())
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_DIAMOND, 1, 1).rare())
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_NETHERITE, 1, 1).rarer())
            .withPool(singleItemPool(SIMPLETMS_BLANK_TM, 1, 1).rarer())
            .withPool(randomArceusPlatePool().setRolls(between(1f, 3f)))
            .withPool(singleItemPool(MegaShowdownItems.WISHING_STAR, 1, 1).common())
            .build()
    }

    fun generateBeekeeperLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(COBBLECUISINE_MILD_HONEY_CURRY, 1, 2).common())
            .withPool(singleItemPool(CobblemonItems.NET_BALL, 5, 15).common())
            .withPool(singleItemPool(CobblemonItems.FLOWER_SWEET, 1, 1).common())
            .withPool(randomBerryPool(between(3f, 7f), between(2f, 3f)).common())
            .withPool(singleItemPool(CobblemonItems.BUG_GEM, 1, 2).rare())
            .withPool(singleItemPool(MegaShowdownItems.MAX_HONEY, 1, 1).rare())
            .withPool(singleItemPool(MegaShowdownItems.DYNAMAX_CANDY, 1, 3).rare())
            .build()
    }

    fun generateCaveCampLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.BIG_ROOT, 10, 20).common())
            .withPool(singleItemPool(CobblemonItems.ENERGY_ROOT, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.DUSK_BALL, 5, 15).common())
            .withPool(singleItemPool(CobblemonItems.ROCK_GEM, 1, 2).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.HARD_STONE))
                .add(optLootItem(CobblemonItems.OVAL_STONE))
                .add(optLootItem(CobblemonItems.EVERSTONE))
                .add(optLootItem(CobblemonItems.EVIOLITE))
                .rare()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.HEAT_ROCK))
                .add(optLootItem(CobblemonItems.ICY_ROCK))
                .add(optLootItem(CobblemonItems.DAMP_ROCK))
                .add(optLootItem(CobblemonItems.SMOOTH_ROCK))
                .rare()
            )
            .build()
    }

    fun generateConduitChurchTreasureLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.WATER_GEM, 1, 2).common())
            .withPool(singleItemPool(CobblemonItems.KINGS_ROCK, 1, 1).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_L, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_XL, 2, 5).rare())
            .withPool(singleItemPool(CobblemonItems.POKEROD_SMITHING_TEMPLATE, 1, 1).rare())
            .withPool(singleItemPool(CobblemonItems.ABILITY_PATCH, 1, 1).rarer())
            .withPool(singleItemPool(MegaShowdownItems.SPARKLING_STONE_DARK, 1, 1))
            .withPool(singleItemPool(MegaShowdownItems.BLANK_Z, 1, 1).common())
            .build()
    }

    fun generateConduitChurchLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.DIVE_BALL, 5, 15).common())
            .withPool(singleItemPool(CobblemonItems.MYSTIC_WATER, 1, 1).rare())
            .withPool(singleItemPool(CobblemonItems.WATER_STONE, 1, 1).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.DRAGON_SCALE))
                .add(optLootItem(CobblemonItems.PRISM_SCALE))
                .add(optLootItem(CobblemonItems.DEEP_SEA_TOOTH))
                .add(optLootItem(CobblemonItems.DEEP_SEA_SCALE))
                .rare()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.HELIX_FOSSIL))
                .add(optLootItem(CobblemonItems.DOME_FOSSIL))
                .add(optLootItem(CobblemonItems.COVER_FOSSIL))
                .rarer()
            )
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .withPool(singleItemPool(BOTTLE_CAP_GOLD, 1, 1).rarer())
            .build()
    }

    fun generateConduitRuinsLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.KINGS_ROCK))
                .add(optLootItem(CobblemonItems.SHELL_BELL))
                .common()
            )
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_L, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.DIVE_BALL, 5, 15).common())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.DRAGON_SCALE))
                .add(optLootItem(CobblemonItems.PRISM_SCALE))
                .add(optLootItem(CobblemonItems.DEEP_SEA_TOOTH))
                .add(optLootItem(CobblemonItems.DEEP_SEA_SCALE))
                .rare()
            )
            .withPool(singleItemPool(CobblemonItems.POKEROD_SMITHING_TEMPLATE, 1, 1).rare())
            .withPool(singleItemPool(CobblemonItems.WATER_GEM, 1, 2).rare())
            .withPool(singleItemPool(MegaShowdownItems.BLANK_Z, 1, 1).common())
            .withPool(singleItemPool(MegaShowdownItems.SPARKLING_STONE_LIGHT, 1, 1).rare())
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .withPool(singleItemPool(BOTTLE_CAP_GOLD, 1, 1).rarer())
            .build()
    }

    fun generateEnchantTowerTopLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_L, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_XL, 2, 5).rare())
            .withPool(singleItemPool(CobblemonItems.RELIC_COIN_POUCH, 2, 5))
            .withPool(singleItemPool(CobblemonItems.DRAGON_GEM, 1, 2).rare())
            .withPool(singleItemPool(CobblemonItems.FAIRY_GEM, 1, 2).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.COVERT_CLOAK))
                .add(optLootItem(CobblemonItems.CLEAR_AMULET))
                .rare()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.DAWN_STONE))
                .add(optLootItem(CobblemonItems.DUSK_STONE))
                .add(optLootItem(CobblemonItems.SHINY_STONE))
                .rare()
            )
            .withPool(singleItemPool(CobblemonItems.ABILITY_PATCH, 1, 1).rarer())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.SCROLL_OF_DARKNESS))
                .add(optLootItem(CobblemonItems.SCROLL_OF_WATERS))
                .rarest()
            )
            .build()
    }

    fun generateEnchantTowerTowerLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.DREAM_BALL, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_XS, 15, 20).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_S, 15, 20).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_M, 10, 15).common())
            .withPool(singleItemPool(CobblemonItems.EXPERIENCE_CANDY_L, 5, 10).rare())
            .withPool(singleItemPool(CobblemonItems.OLD_GATEAU, 1, 2).common())
            .withPool(singleItemPool(CobblemonItems.RELIC_COIN, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.MENTAL_HERB, 1, 1).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.CRACKED_POT))
                .add(optLootItem(CobblemonItems.UNREMARKABLE_TEACUP))
                .rare()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.CHIPPED_POT))
                .add(optLootItem(CobblemonItems.MASTERPIECE_TEACUP))
                .rarer()
            )
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .withPool(singleItemPool(BOTTLE_CAP_GOLD, 1, 1).rarer())
            .build()
    }

    fun generateGolemHouseNormalLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.FRIEND_BALL, 5, 15).common())
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(CobblemonItems.METAL_COAT))
                    .add(optLootItem(CobblemonItems.METAL_ALLOY))
                    .rare()
            )
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS))
                    .add(optLootItem(CobblemonItems.SAFETY_GOGGLES))
                    .add(optLootItem(CobblemonItems.SOOTHE_BELL))
                    .add(optLootItem(CobblemonItems.ROCKY_HELMET))
                    .rare()
            )
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .build()
    }

    fun generateGolemHouseZombieLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.MOON_BALL, 5, 15).common())
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(CobblemonItems.METAL_COAT))
                    .add(optLootItem(CobblemonItems.METAL_ALLOY))
                    .rare()
            )
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS))
                    .add(optLootItem(CobblemonItems.BLACK_SLUDGE))
                    .add(optLootItem(CobblemonItems.SAFETY_GOGGLES))
                    .add(optLootItem(CobblemonItems.ROCKY_HELMET))
                    .rare()
            )
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .build()
    }

    fun generateNoteblockLabBasementLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.TIMER_BALL, 5, 10).common())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.THROAT_SPRAY))
                .add(optLootItem(CobblemonItems.METRONOME))
                .common()
            )
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_COPPER, 2, 3).common())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(TMCRAFT_BLANK_DISC_IRON).count(between(1f, 2f)))
                .add(optLootItem(SIMPLETMS_BLANK_TR).count(between(1f, 2f)))
                .common()
            )
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_GOLD, 1, 1).rare())
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(TMCRAFT_BLANK_DISC_EMERALD))
                    .add(optLootItem(SIMPLETMS_BLANK_TM))
                    .rarer()
            )
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_DIAMOND, 1, 1).rarer())
            .withPool(singleItemPool(MegaShowdownItems.BLANK_Z, 1, 1).rare())
            .build()
    }

    fun generateNoteblockLabHouseLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.NORMAL_GEM, 1, 2).rare())
            .withPool(singleItemPool(CobblemonItems.FAIRY_GEM, 1, 2).rare())
            .withPool(singleItemPool(CobblemonItems.DESTINY_KNOT, 1, 1).common())
            .withPool(singleItemPool(CobblemonItems.CLEAR_AMULET, 1, 1).rarer())
            .withPool(singleItemPool(CobblemonItems.LUCKY_EGG, 1, 1).rarer())
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .build()
    }

    fun generateNoteblockShipBarrelsLoot(): LootTable {
        return lootTable()
            .withPool(randomBerryPool(between(2f, 3f), between(2f, 5f)).common())
            .withPool(singleItemPool(CobblemonItems.LEFTOVERS, 1, 1).rare())
            .withPool(singleItemPool(CobblemonItems.MEDICINAL_LEEK, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.REVIVAL_HERB, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.PEP_UP_FLOWER, 1, 5).common())
            .withPool(singleItemPool(CobblemonItems.HEARTY_GRAINS, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.ANCIENT_WING_BALL, 1, 5).common())
            .build()
    }

    fun generateNoteblockShipChestLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.FLYING_GEM, 1, 2).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.AIR_BALLOON))
                .add(optLootItem(CobblemonItems.THROAT_SPRAY))
                .add(optLootItem(CobblemonItems.METRONOME))
                .common()
            )
            .withPool(singleItemPool(CobblemonItems.SHARP_BEAK, 1, 1).common())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(TMCRAFT_BLANK_DISC_IRON).count(between(1f, 2f)))
                .add(optLootItem(SIMPLETMS_BLANK_TR).count(between(1f, 2f)))
                .common()
            )
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_GOLD, 1, 1).common())
            .withPool(
                lootPoolSingleRoll()
                    .add(optLootItem(TMCRAFT_BLANK_DISC_EMERALD))
                    .add(optLootItem(SIMPLETMS_BLANK_TM))
                    .rare()
            )
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_DIAMOND, 1, 1).rare())
            .withPool(singleItemPool(MegaShowdownItems.BLANK_Z, 1, 1).rare())
            .build()
    }

    fun generateResearcherTentLabLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(MegaShowdownItems.ZYGARDE_CELL, 1, 1))
            .withPool(singleItemPool(CobblemonItems.ANCIENT_POKE_BALL, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.ANCIENT_GREAT_BALL, 5, 10).common())
            .withPool(singleItemPool(CobblemonItems.ANCIENT_ULTRA_BALL, 1, 5).common())
            .withPool(singleItemPool(MegaShowdownItems.ZYGARDE_CELL, 1, 2).common())
            .withPool(singleItemPool(MegaShowdownItems.ZYGARDE_CORE, 1, 1).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.FLAME_ORB))
                .add(optLootItem(CobblemonItems.TOXIC_ORB))
                .common()
            )
            .withPool(singleItemPool(CobblemonItems.LIFE_ORB, 1, 1).rare())
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.WIDE_LENS))
                .add(optLootItem(CobblemonItems.WISE_GLASSES))
                .common()
            )
            .build()
    }

    fun generateResearcherTentTentLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll()
                .add(expandTag(CobblemonItemTags.FOSSILS))
                .rarer()
            )
            .withPool(singleItemPool(CobblemonItems.RELIC_COIN, 5, 15).common())
            .withPool(singleItemPool(CobblemonItems.RELIC_COIN_POUCH, 1, 2).rare())
            .withPool(lootPoolSingleRoll()
                .add(expandTag(CobblemonItemTags.TYPE_GEMS))
                .rare()
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.BLUNDER_POLICY))
                .add(optLootItem(CobblemonItems.WEAKNESS_POLICY))
                .rare()
            )
            .withPool(singleItemPool(BOTTLE_CAP_SILVER, 1, 1).rare())
            .withPool(singleItemPool(MegaShowdownItems.TERA_ORB, 1, 1).rare())
            .build()
    }

    private fun randomArceusPlatePool() = lootPoolSingleRoll().also { pool ->
        listOf(
            MegaShowdownItems.FLAME_PLATE,
            MegaShowdownItems.SPLASH_PLATE,
            MegaShowdownItems.ZAP_PLATE,
            MegaShowdownItems.MEADOW_PLATE,
            MegaShowdownItems.ICICLE_PLATE,
            MegaShowdownItems.FIST_PLATE,
            MegaShowdownItems.TOXIC_PLATE,
            MegaShowdownItems.EARTH_PLATE,
            MegaShowdownItems.SKY_PLATE,
            MegaShowdownItems.MIND_PLATE,
            MegaShowdownItems.INSECT_PLATE,
            MegaShowdownItems.STONE_PLATE,
            MegaShowdownItems.SPOOKY_PLATE,
            MegaShowdownItems.DRACO_PLATE,
            MegaShowdownItems.DREAD_PLATE,
            MegaShowdownItems.IRON_PLATE,
            MegaShowdownItems.PIXIE_PLATE,
        ).forEach { pool.add(optLootItem(it)) }
    }

    private fun randomBerryPool(rolls: NumberProvider, countForRoll: NumberProvider) = lootPool()
        .setRolls(rolls)
        .add(expandTag(GrowssethTags.COBBLEMON_BERRIES_EXCEPT_RARE).count(countForRoll))

    private fun singleItemPool(item: ItemLike, min: Int, max: Int): LootPool.Builder {
        return lootPoolSingleRoll()
            .add(optLootItem(item).count(between(min.toFloat(), max.toFloat())))
    }
    private fun singleItemPool(itemSupplier: RegistrySupplier<Item>, min: Int, max: Int): LootPool.Builder {
        return lootPoolSingleRoll()
            .add(optLootItem(itemSupplier).count(between(min.toFloat(), max.toFloat())))
    }
    private fun singleItemPool(id: ResourceLocation, min: Int, max: Int): LootPool.Builder {
        return lootPoolSingleRoll()
            .add(optLootItem(id).count(between(min.toFloat(), max.toFloat())))
   }

    private fun LootPool.Builder.common() = this.withRandomChance(CHANCE_COMMON)
    private fun LootPool.Builder.rare() = this.withRandomChance(CHANCE_RARE)
    private fun LootPool.Builder.rarer() = this.withRandomChance(CHANCE_RARER)
    private fun LootPool.Builder.rarest() = this.withRandomChance(CHANCE_RAREST)

    private fun optLootItem(item: ItemLike) = expandTag(optionalLootItemTags.optionalModLootTableItem(item))
    private fun optLootItem(itemSupplier: RegistrySupplier<Item>) = optLootItem(itemSupplier.get())
    private fun optLootItem(id: ResourceLocation) = expandTag(optionalLootItemTags.optionalModLootTableItem(id))
}