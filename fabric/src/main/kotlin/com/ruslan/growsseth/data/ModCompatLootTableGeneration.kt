//package com.ruslan.growsseth.data
//
//import com.cobblemon.mod.common.CobblemonItems
//import com.github.yajatkaul.mega_showdown.item.MegaShowdownItems
//import com.ruslan.growsseth.GrowssethLootTables
//import com.ruslan.growsseth.compat.data.OptionalLootItemTags
//import com.ruslan.growsseth.item.GrowssethItems
//import dev.architectury.registry.registries.RegistrySupplier
//import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
//import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider
//import net.minecraft.core.HolderLookup
//import net.minecraft.resources.ResourceKey
//import net.minecraft.world.item.Item
//import net.minecraft.world.item.Items
//import net.minecraft.world.level.ItemLike
//import net.minecraft.world.level.storage.loot.LootTable
//import net.minecraft.world.level.storage.loot.LootTable.lootTable
//import net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem
//import net.minecraft.world.level.storage.loot.entries.TagEntry.expandTag
//import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
//import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between
//import java.util.concurrent.CompletableFuture
//import java.util.function.BiConsumer
//
//
//class ModCompatMiscLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.EMPTY) {
//
//    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {
//        output.accept(
//            GrowssethLootTables.COBBLEMON_DEFEAT_RESEARCHER,
//            lootTable()
//                .withPool(lootPoolSingleRoll()
//                    .add(lootTableItem(Items.ENCHANTED_GOLDEN_APPLE))
//                    .add(lootTableItem(Items.NETHERITE_SCRAP).count(between(1f, 4f)))
//                )
//                .withPool(lootPoolSingleRoll()
//                    .add(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
//                        .count(between(0f, 2f))
//                    )
//                )
//        )
//    }
//
//    override fun getName(): String = "GrowssethModCompatMiscLootTable"
//}
//
//class ModCompatStructureLootTableGeneration(val registries: CompletableFuture<HolderLookup.Provider>, val optionalLootItemTags: OptionalLootItemTags) {
//    fun generateForgeLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.HEAVY_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.BLUE_APRICORN).count(between(5f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.BLACK_APRICORN).count(between(5f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.METAL_COAT).setWeight(2))
//                .add(optLootItem(CobblemonItems.BLACK_AUGURITE).setWeight(2))
//                .add(optLootItem(CobblemonItems.PROTECTOR).setWeight(2))
//                .add(optLootItem(CobblemonItems.AUSPICIOUS_ARMOR).setWeight(2))
//                .add(optLootItem(CobblemonItems.MALICIOUS_ARMOR).setWeight(2))
//                .add(optLootItem(CobblemonItems.SHELL_HELMET).setWeight(2))
//                .add(optLootItem(CobblemonItems.METAL_ALLOY).setWeight(2))
//                .add(optLootItem(CobblemonItems.STEEL_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.UPGRADE).setWeight(10)) // Smithing Template (move upgrade)? assuming Upgrade is what they meant
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(10))
////                .add(optLootItem(CobblemonItems.GOLD_BOTTLE_CAP).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateForgeSecretLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.RELIC_COIN).setWeight(10))
////                .add(optLootItem(CobblemonItems.GOLD_GILDED_CHEST).setWeight(2)) // Gold Blank Disk?
//                .add(optLootItem(MegaShowdownItems.WISHING_STAR).setWeight(10))
//            )
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(MegaShowdownItems.FLAME_PLATE))
//                .add(optLootItem(MegaShowdownItems.SPLASH_PLATE))
//                .add(optLootItem(MegaShowdownItems.ZAP_PLATE))
//                .add(optLootItem(MegaShowdownItems.MEADOW_PLATE))
//                .add(optLootItem(MegaShowdownItems.ICICLE_PLATE))
//                .add(optLootItem(MegaShowdownItems.FIST_PLATE))
//                .add(optLootItem(MegaShowdownItems.TOXIC_PLATE))
//                .add(optLootItem(MegaShowdownItems.EARTH_PLATE))
//                .add(optLootItem(MegaShowdownItems.SKY_PLATE))
//                .add(optLootItem(MegaShowdownItems.MIND_PLATE))
//                .add(optLootItem(MegaShowdownItems.INSECT_PLATE))
//                .add(optLootItem(MegaShowdownItems.STONE_PLATE))
//                .add(optLootItem(MegaShowdownItems.SPOOKY_PLATE))
//                .add(optLootItem(MegaShowdownItems.DRACO_PLATE))
//                .add(optLootItem(MegaShowdownItems.DREAD_PLATE))
//                .add(optLootItem(MegaShowdownItems.IRON_PLATE))
//                .add(optLootItem(MegaShowdownItems.PIXIE_PLATE))
//            )
//            .build()
//    }
//
//    fun generateBeekeeperLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.NET_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.FLOWER_SWEET).setWeight(10))
//                .add(optLootItem(CobblemonItems.ORAN_BERRY).count(between(10f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.SITRUS_BERRY).count(between(10f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.BUG_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(MegaShowdownItems.MAX_HONEY).setWeight(2))
//                .add(optLootItem(MegaShowdownItems.DYNAMAX_CANDY).count(between(1f, 3f)).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateCaveCampLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.BIG_ROOT).count(between(10f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.ENERGY_ROOT).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.DUSK_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.ROCK_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.HARD_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.OVAL_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.EVERSTONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.EVIOLITE).setWeight(2))
//                .add(optLootItem(CobblemonItems.HEAT_ROCK).setWeight(2))
//                .add(optLootItem(CobblemonItems.ICY_ROCK).setWeight(2))
//                .add(optLootItem(CobblemonItems.DAMP_ROCK).setWeight(2))
//                .add(optLootItem(CobblemonItems.SMOOTH_ROCK).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateConduitChurchTreasureLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(MegaShowdownItems.SPARKLING_STONE_DARK))
//            )
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.WATER_GEM).count(between(1f, 2f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.KINGS_ROCK).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_XL).count(between(2f, 5f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.POKEROD_SMITHING_TEMPLATE).setWeight(2))
//                .add(optLootItem(CobblemonItems.ABILITY_PATCH).setWeight(1))
//                .add(optLootItem(MegaShowdownItems.BLANK_Z).setWeight(10))
//            )
//            .build()
//    }
//
//    fun generateConduitChurchLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.DIVE_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.MYSTIC_WATER).setWeight(2))
//                .add(optLootItem(CobblemonItems.WATER_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.DRAGON_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.PRISM_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.DEEP_SEA_TOOTH).setWeight(2))
//                .add(optLootItem(CobblemonItems.DEEP_SEA_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.HELIX_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.DOME_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.COVER_FOSSIL).setWeight(1))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
////                .add(optLootItem(CobblemonItems.GOLD_BOTTLE_CAP).setWeight(1))
//            )
//            .build()
//    }
//
//    fun generateConduitRuinsLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.KINGS_ROCK).setWeight(10))
//                .add(optLootItem(CobblemonItems.SHELL_BELL).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.DIVE_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.DRAGON_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.PRISM_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.DEEP_SEA_TOOTH).setWeight(2))
//                .add(optLootItem(CobblemonItems.DEEP_SEA_SCALE).setWeight(2))
//                .add(optLootItem(CobblemonItems.POKEROD_SMITHING_TEMPLATE).setWeight(2))
//                .add(optLootItem(CobblemonItems.WATER_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(MegaShowdownItems.BLANK_Z).setWeight(10))
//                .add(optLootItem(MegaShowdownItems.SPARKLING_STONE_LIGHT).setWeight(2))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
////                .add(optLootItem(CobblemonItems.GOLD_BOTTLE_CAP).setWeight(1))
//            )
//            .build()
//    }
//
//    fun generateEnchantTowerTopLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_XL).count(between(2f, 5f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.RELIC_COIN_POUCH).count(between(2f, 5f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.DRAGON_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.FAIRY_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.COVERT_CLOAK).setWeight(2))
//                .add(optLootItem(CobblemonItems.CLEAR_AMULET).setWeight(2))
//                .add(optLootItem(CobblemonItems.DAWN_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.DUSK_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.SHINY_STONE).setWeight(2))
//                .add(optLootItem(CobblemonItems.ABILITY_PATCH).setWeight(1))
//                .add(optLootItem(CobblemonItems.SCROLL_OF_DARKNESS).setWeight(1))
//                .add(optLootItem(CobblemonItems.SCROLL_OF_WATERS).setWeight(1))
//            )
//            .build()
//    }
//
//    fun generateEnchantTowerTowerLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.DREAM_BALL).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_XS).count(between(15f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_S).count(between(15f, 20f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_M).count(between(10f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.OLD_GATEAU).count(between(1f, 2f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.RELIC_COIN).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.MENTAL_HERB).setWeight(2))
//                .add(optLootItem(CobblemonItems.CRACKED_POT).setWeight(2))
//                .add(optLootItem(CobblemonItems.UNREMARKABLE_TEACUP).setWeight(2))
//                .add(optLootItem(CobblemonItems.CHIPPED_POT).setWeight(1))
//                .add(optLootItem(CobblemonItems.MASTERPIECE_TEACUP).setWeight(1))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
////                .add(optLootItem(CobblemonItems.GOLD_BOTTLE_CAP).setWeight(1))
//            )
//            .build()
//    }
//
//    fun generateGolemHouseNormalLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.FRIEND_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.METAL_COAT).setWeight(2))
//                .add(optLootItem(CobblemonItems.METAL_ALLOY).setWeight(2))
//                .add(optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS).setWeight(2))
//                .add(optLootItem(CobblemonItems.SAFETY_GOGGLES).setWeight(2))
//                .add(optLootItem(CobblemonItems.SOOTHE_BELL).setWeight(2))
//                .add(optLootItem(CobblemonItems.ROCKY_HELMET).setWeight(2))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateGolemHouseZombieLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.MOON_BALL).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.METAL_COAT).setWeight(2))
//                .add(optLootItem(CobblemonItems.METAL_ALLOY).setWeight(2))
//                .add(optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS).setWeight(2))
//                .add(optLootItem(CobblemonItems.BLACK_SLUDGE).setWeight(2))
//                .add(optLootItem(CobblemonItems.SAFETY_GOGGLES).setWeight(2))
//                .add(optLootItem(CobblemonItems.ROCKY_HELMET).setWeight(2))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateNoteblockLabBasementLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.TIMER_BALL).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.THROAT_SPRAY).setWeight(10))
//                .add(optLootItem(CobblemonItems.METRONOME).setWeight(10))
////                .add(optLootItem(CobblemonItems.TUMBLESTONE).count(between(2f, 3f)).setWeight(10)) // Copper Blank Disk? mapping to Tumblestone
////                .add(optLootItem(CobblemonItems.IRON_TUMBLESTONE).count(between(1f, 2f)).setWeight(10)) // Iron Blank Disk
//                .add(optLootItem(MegaShowdownItems.BLANK_Z).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateNoteblockLabHouseLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.NORMAL_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.FAIRY_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.DESTINY_KNOT).setWeight(10))
//                .add(optLootItem(CobblemonItems.CLEAR_AMULET).setWeight(1))
//                .add(optLootItem(CobblemonItems.LUCKY_EGG).setWeight(1))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateNoteblockShipBarrelsLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.ORAN_BERRY).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.SITRUS_BERRY).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.LEFTOVERS).setWeight(2))
//                .add(optLootItem(CobblemonItems.MEDICINAL_LEEK).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.REVIVAL_HERB).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.PEP_UP_FLOWER).count(between(1f, 5f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.HEARTY_GRAINS).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.ANCIENT_WING_BALL).count(between(1f, 5f)).setWeight(10))
//            )
//            .build()
//    }
//
//    fun generateNoteblockShipChestLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.FLYING_GEM).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.AIR_BALLOON).setWeight(10))
//                .add(optLootItem(CobblemonItems.THROAT_SPRAY).setWeight(10))
//                .add(optLootItem(CobblemonItems.METRONOME).setWeight(10))
//                .add(optLootItem(CobblemonItems.SHARP_BEAK).setWeight(10))
//                .add(optLootItem(MegaShowdownItems.BLANK_Z).setWeight(2))
//            )
//            .build()
//    }
//
//    fun generateResearcherTentLabLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(MegaShowdownItems.ZYGARDE_CELL).setWeight(10)) // Assured 1 in separate pool later?
//            )
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.ANCIENT_POKE_BALL).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.ANCIENT_GREAT_BALL).count(between(5f, 10f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.ANCIENT_ULTRA_BALL).count(between(1f, 5f)).setWeight(10))
//                .add(optLootItem(MegaShowdownItems.ZYGARDE_CELL).count(between(1f, 2f)).setWeight(10))
//                .add(optLootItem(MegaShowdownItems.ZYGARDE_CORE).setWeight(2))
//                .add(optLootItem(CobblemonItems.FLAME_ORB).setWeight(10))
//                .add(optLootItem(CobblemonItems.TOXIC_ORB).setWeight(10))
//                .add(optLootItem(CobblemonItems.LIFE_ORB).setWeight(2))
//                .add(optLootItem(CobblemonItems.WIDE_LENS).setWeight(10))
//                .add(optLootItem(CobblemonItems.WISE_GLASSES).setWeight(10))
//            )
//            .build()
//    }
//
//    fun generateResearcherTentTentLoot(): LootTable {
//        return lootTable()
//            .withPool(lootPoolSingleRoll()
//                .add(optLootItem(CobblemonItems.ARMOR_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.CLAW_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.COVER_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.DOME_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.HELIX_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.JAW_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.PLUME_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.ROOT_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.SAIL_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.SKULL_FOSSIL).setWeight(1))
//                .add(optLootItem(CobblemonItems.RELIC_COIN).count(between(5f, 15f)).setWeight(10))
//                .add(optLootItem(CobblemonItems.RELIC_COIN_POUCH).count(between(1f, 2f)).setWeight(2))
//                .add(optLootItem(CobblemonItems.NORMAL_GEM).setWeight(2))
//                .add(optLootItem(CobblemonItems.FIRE_GEM).setWeight(2))
//                .add(optLootItem(CobblemonItems.WATER_GEM).setWeight(2))
//                .add(optLootItem(CobblemonItems.BLUNDER_POLICY).setWeight(2))
//                .add(optLootItem(CobblemonItems.WEAKNESS_POLICY).setWeight(2))
////                .add(optLootItem(CobblemonItems.SILVER_BOTTLE_CAP).setWeight(2))
//                .add(optLootItem(MegaShowdownItems.TERA_ORB).setWeight(1))
//            )
//            .build()
//    }
//
//    private fun optLootItem(item: ItemLike) = expandTag(optionalLootItemTags.optionalModLootTableItem(item))
//    private fun optLootItem(itemSupplier: RegistrySupplier<Item>)
//        = optLootItem(itemSupplier.get())
//}