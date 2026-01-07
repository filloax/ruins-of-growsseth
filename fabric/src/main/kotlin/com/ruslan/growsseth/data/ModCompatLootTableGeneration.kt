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
    }

    fun generateForgeLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.HEAVY_BALL, between(2f, 3f), 2, 5))
            .withPool(lootPool()
                .setRolls(between(2f, 6f))
                .add(optLootItem(CobblemonItems.BLUE_APRICORN).count(between(2f, 4f)))
                .add(optLootItem(CobblemonItems.BLACK_APRICORN).count(between(2f, 4f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.METAL_COAT),
                optLootItem(CobblemonItems.METAL_COAT),
                optLootItem(CobblemonItems.BLACK_AUGURITE),
                optLootItem(CobblemonItems.PROTECTOR),
                optLootItem(CobblemonItems.AUSPICIOUS_ARMOR),
                optLootItem(CobblemonItems.MALICIOUS_ARMOR),
                optLootItem(CobblemonItems.SHELL_HELMET),
                optLootItem(CobblemonItems.METAL_ALLOY),
                ))
            .withPool(lootPoolSingleRoll().add(optLootItem(BOTTLE_CAP_SILVER)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_GOLD)))
            // TODO: smithing template move upgrade
            .build()
    }

    fun generateForgeSecretLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_IRON, between(1f, 2f), 1, 2))
            .withPool(singleItemPool(SIMPLETMS_BLANK_TR, between(1f, 2f), 1, 2))
            .withPool(singleItemPool(TMCRAFT_BLANK_DISC_GOLD, exactly(1f), 1, 2))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(TMCRAFT_BLANK_DISC_DIAMOND)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(TMCRAFT_BLANK_DISC_NETHERITE)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(SIMPLETMS_BLANK_TM)))
            .withPool(randomArceusPlatePool().setRolls(between(1f, 3f)))
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.WISHING_STAR)))
            .build()
    }

    fun generateBeekeeperLoot(): LootTable {
        return lootTable()
            // TODO: Mild Honey Curry 1-2
            .withPool(singleItemPool(CobblemonItems.NET_BALL, between(2f, 3f), 2, 5))
            .withPool(lootPoolSingleRoll().add(optLootItem(CobblemonItems.FLOWER_SWEET)))
            .withPool(randomBerryPool(between(3f, 7f), between(2f, 3f)))
            // reduced chance from base doc as there are many barrels
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.BUG_GEM).count(between(1f, 2f)))
                .withRandomChance(CHANCE_RARER)
            )
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.MAX_HONEY))
                .withRandomChance(CHANCE_RAREST)
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(MegaShowdownItems.DYNAMAX_CANDY).count(between(1f, 3f)))
                .withRandomChance(CHANCE_RAREST)
            )
            .build()
    }

    fun generateCaveCampLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.BIG_ROOT, between(2f, 3f), 5, 8))
            .withPool(singleItemPool(CobblemonItems.ENERGY_ROOT, exactly(2f), 2, 5))
            .withPool(singleItemPool(CobblemonItems.DUSK_BALL, between(2f, 3f), 2, 5))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.ROCK_GEM).count(between(1f, 2f)))
                .withRandomChance(CHANCE_RARE)
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.HARD_STONE),
                optLootItem(CobblemonItems.OVAL_STONE),
                optLootItem(CobblemonItems.EVERSTONE),
                optLootItem(CobblemonItems.EVIOLITE),
                ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.HEAT_ROCK),
                optLootItem(CobblemonItems.ICY_ROCK),
                optLootItem(CobblemonItems.DAMP_ROCK),
                optLootItem(CobblemonItems.SMOOTH_ROCK),
                ))
            .build()
    }

    fun generateConduitChurchTreasureLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.WATER_GEM).count(between(1f, 2f)))
            )
            .withPool(lootPoolSingleRoll().add(optLootItem(CobblemonItems.KINGS_ROCK)))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.EXPERIENCE_CANDY_XL).count(between(2f, 5f))
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(CobblemonItems.POKEROD_SMITHING_TEMPLATE)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(CobblemonItems.ABILITY_PATCH)))
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.SPARKLING_STONE_DARK)))
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.BLANK_Z)))
            .build()
    }

    fun generateConduitChurchLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.DIVE_BALL, between(2f, 3f), 2, 5))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(CobblemonItems.MYSTIC_WATER)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(CobblemonItems.WATER_STONE)))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.DRAGON_SCALE),
                optLootItem(CobblemonItems.PRISM_SCALE),
                optLootItem(CobblemonItems.DEEP_SEA_TOOTH),
                optLootItem(CobblemonItems.DEEP_SEA_SCALE)
            ))
            .withPool(randomChancePool(CHANCE_RARER,
                optLootItem(CobblemonItems.HELIX_FOSSIL),
                optLootItem(CobblemonItems.DOME_FOSSIL),
                optLootItem(CobblemonItems.COVER_FOSSIL)
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(BOTTLE_CAP_GOLD)))
            .build()
    }

    fun generateConduitRuinsLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.KINGS_ROCK))
                .add(optLootItem(CobblemonItems.SHELL_BELL))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)))
            )
            .withPool(singleItemPool(CobblemonItems.DIVE_BALL, between(2f, 3f), 2, 5))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.DRAGON_SCALE),
                optLootItem(CobblemonItems.PRISM_SCALE),
                optLootItem(CobblemonItems.DEEP_SEA_TOOTH),
                optLootItem(CobblemonItems.DEEP_SEA_SCALE)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.POKEROD_SMITHING_TEMPLATE)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.WATER_GEM).count(between(1f, 2f))
            ))
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.BLANK_Z)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(MegaShowdownItems.SPARKLING_STONE_LIGHT)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(BOTTLE_CAP_GOLD)))
            .build()
    }

    fun generateEnchantTowerTopLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.EXPERIENCE_CANDY_XL).count(between(2f, 5f))
            ))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.RELIC_COIN_POUCH).count(between(2f, 5f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.DRAGON_GEM).count(between(1f, 2f))
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.FAIRY_GEM).count(between(1f, 2f))
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.COVERT_CLOAK),
                optLootItem(CobblemonItems.CLEAR_AMULET)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.DAWN_STONE),
                optLootItem(CobblemonItems.DUSK_STONE),
                optLootItem(CobblemonItems.SHINY_STONE)
            ))
            .withPool(randomChancePool(CHANCE_RARER,
                optLootItem(CobblemonItems.ABILITY_PATCH)
            ))
            .withPool(randomChancePool(CHANCE_RAREST,
                optLootItem(CobblemonItems.SCROLL_OF_DARKNESS),
                optLootItem(CobblemonItems.SCROLL_OF_WATERS)
            ))
            .build()
    }

    fun generateEnchantTowerTowerLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.DREAM_BALL, between(2f, 3f), 2, 3))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_XS).count(between(15f, 20f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_S).count(between(15f, 20f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.EXPERIENCE_CANDY_M).count(between(10f, 15f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.EXPERIENCE_CANDY_L).count(between(5f, 10f))
            ))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.OLD_GATEAU).count(between(1f, 2f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.RELIC_COIN).count(between(5f, 10f)))
            )
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(CobblemonItems.MENTAL_HERB)))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.CRACKED_POT),
                optLootItem(CobblemonItems.UNREMARKABLE_TEACUP)
            ))
            .withPool(randomChancePool(CHANCE_RARER,
                optLootItem(CobblemonItems.CHIPPED_POT),
                optLootItem(CobblemonItems.MASTERPIECE_TEACUP)
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(BOTTLE_CAP_GOLD)))
            .build()
    }

    fun generateGolemHouseNormalLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.FRIEND_BALL, between(2f, 3f), 2, 5))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.METAL_COAT),
                optLootItem(CobblemonItems.METAL_ALLOY)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS),
                optLootItem(CobblemonItems.SAFETY_GOGGLES),
                optLootItem(CobblemonItems.SOOTHE_BELL),
                optLootItem(CobblemonItems.ROCKY_HELMET)
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .build()
    }

    fun generateGolemHouseZombieLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.MOON_BALL, between(2f, 3f), 2, 5))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.METAL_COAT),
                optLootItem(CobblemonItems.METAL_ALLOY)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.HEAVY_DUTY_BOOTS),
                optLootItem(CobblemonItems.BLACK_SLUDGE),
                optLootItem(CobblemonItems.SAFETY_GOGGLES),
                optLootItem(CobblemonItems.ROCKY_HELMET)
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .build()
    }

    fun generateNoteblockLabBasementLoot(): LootTable {
        return lootTable()
            .withPool(singleItemPool(CobblemonItems.TIMER_BALL, between(2f, 3f), 2, 3))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.THROAT_SPRAY))
                .add(optLootItem(CobblemonItems.METRONOME))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(TMCRAFT_BLANK_DISC_COPPER).count(between(2f, 3f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(TMCRAFT_BLANK_DISC_IRON).count(between(1f, 2f)))
                .add(optLootItem(SIMPLETMS_BLANK_TR).count(between(1f, 2f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(TMCRAFT_BLANK_DISC_GOLD),
            ))
            .withPool(randomChancePool(CHANCE_RARER,
                optLootItem(TMCRAFT_BLANK_DISC_EMERALD),
                        optLootItem(SIMPLETMS_BLANK_TM)
            ))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(TMCRAFT_BLANK_DISC_DIAMOND)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(MegaShowdownItems.BLANK_Z)))
            .build()
    }

    fun generateNoteblockLabHouseLoot(): LootTable {
        return lootTable()
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.NORMAL_GEM).count(between(1f, 2f))
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.FAIRY_GEM).count(between(1f, 2f))
            ))
            .withPool(lootPoolSingleRoll().add(optLootItem(CobblemonItems.DESTINY_KNOT)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(CobblemonItems.CLEAR_AMULET)))
            .withPool(randomChancePool(CHANCE_RARER, optLootItem(CobblemonItems.LUCKY_EGG)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(BOTTLE_CAP_SILVER)))
            .build()
    }

    fun generateNoteblockShipBarrelsLoot(): LootTable {
        return lootTable()
            .withPool(randomBerryPool(between(2f, 3f), between(2f, 5f)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(CobblemonItems.LEFTOVERS)))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.MEDICINAL_LEEK).count(between(5f, 10f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.REVIVAL_HERB).count(between(5f, 10f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.PEP_UP_FLOWER).count(between(1f, 5f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.HEARTY_GRAINS).count(between(5f, 10f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.ANCIENT_WING_BALL).count(between(1f, 5f)))
            )
            .build()
    }

    fun generateNoteblockShipChestLoot(): LootTable {
        return lootTable()
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.FLYING_GEM).count(between(1f, 2f))
            ))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.AIR_BALLOON))
                .add(optLootItem(CobblemonItems.THROAT_SPRAY))
                .add(optLootItem(CobblemonItems.METRONOME))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.SHARP_BEAK))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(TMCRAFT_BLANK_DISC_IRON).count(between(1f, 2f)))
                .add(optLootItem(SIMPLETMS_BLANK_TR).count(between(1f, 2f)))
            )
            .withPool(lootPoolSingleRoll().add(optLootItem(TMCRAFT_BLANK_DISC_GOLD)))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(TMCRAFT_BLANK_DISC_EMERALD),
                    optLootItem(SIMPLETMS_BLANK_TM),
            ))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(TMCRAFT_BLANK_DISC_DIAMOND)))
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(MegaShowdownItems.BLANK_Z)))
            .build()
    }

    fun generateResearcherTentLabLoot(): LootTable {
        return lootTable()
            .withPool(lootPoolSingleRoll().add(optLootItem(MegaShowdownItems.ZYGARDE_CELL)))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.ANCIENT_POKE_BALL).count(between(5f, 10f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.ANCIENT_GREAT_BALL).count(between(5f, 10f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.ANCIENT_ULTRA_BALL).count(between(1f, 5f)))
            )
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(MegaShowdownItems.ZYGARDE_CELL).count(between(1f, 2f)))
            )
            .withPool(randomChancePool(CHANCE_RARE, optLootItem(MegaShowdownItems.ZYGARDE_CORE)))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.FLAME_ORB))
                .add(optLootItem(CobblemonItems.TOXIC_ORB))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.LIFE_ORB)
            ))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.WIDE_LENS))
                .add(optLootItem(CobblemonItems.WISE_GLASSES))
            )
            .build()
    }

    fun generateResearcherTentTentLoot(): LootTable {
        return lootTable()
            .withPool(randomChancePool(CHANCE_RARER,
                expandTag(CobblemonItemTags.FOSSILS)
            ))
            .withPool(lootPoolSingleRoll()
                .add(optLootItem(CobblemonItems.RELIC_COIN).count(between(5f, 15f)))
            )
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.RELIC_COIN_POUCH).count(between(1f, 2f))
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                expandTag(CobblemonItemTags.TYPE_GEMS)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(CobblemonItems.BLUNDER_POLICY),
                optLootItem(CobblemonItems.WEAKNESS_POLICY)
            ))
            .withPool(randomChancePool(CHANCE_RARE,
                optLootItem(BOTTLE_CAP_SILVER)
            ))
            .withPool(randomChancePool(CHANCE_RARER,
                optLootItem(MegaShowdownItems.TERA_ORB)
            ))
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

    private fun singleItemPool(item: ItemLike, rolls: NumberProvider, minForRoll: Int, maxForRoll: Int): LootPool.Builder {
        return lootPool()
            .setRolls(rolls)
            .add(optLootItem(item).count(between(minForRoll.toFloat(), maxForRoll.toFloat())))
    }
    private fun singleItemPool(itemSupplier: RegistrySupplier<Item>, rolls: NumberProvider, minForRoll: Int, maxForRoll: Int): LootPool.Builder {
        return lootPool()
            .setRolls(rolls)
            .add(optLootItem(itemSupplier).count(between(minForRoll.toFloat(), maxForRoll.toFloat())))
    }
    private fun singleItemPool(id: ResourceLocation, rolls: NumberProvider, minForRoll: Int, maxForRoll: Int): LootPool.Builder {
        return lootPool()
            .setRolls(rolls)
            .add(optLootItem(id).count(between(minForRoll.toFloat(), maxForRoll.toFloat())))
    }

    private fun optLootItem(item: ItemLike) = expandTag(optionalLootItemTags.optionalModLootTableItem(item))
    private fun optLootItem(itemSupplier: RegistrySupplier<Item>) = optLootItem(itemSupplier.get())
    private fun optLootItem(id: ResourceLocation) = expandTag(optionalLootItemTags.optionalModLootTableItem(id))
}