package com.ruslan.growsseth.data

import com.ruslan.growsseth.GrowssethLootTables
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.item.GrowssethItems
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.storage.loot.LootPool.lootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.LootTable.lootTable
import net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.TagEntry
import net.minecraft.world.level.storage.loot.functions.*
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

class EntityLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.ENTITY) {
    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {
        listOf(
            GrowssethEntities.RESEARCHER,
            GrowssethEntities.ZOMBIE_RESEARCHER,
        ).map { it.defaultLootTable }.forEach { key ->
            output.accept(key, lootTable()
                .withPool(lootPool()
                    .setRolls(exactly(1.0f))
                    .add(lootTableItem(GrowssethItems.RESEARCHER_DAGGER)
                        .withEnchantments {
                            it.withEnchantment(enchantment(Enchantments.UNBREAKING), exactly(3f))
                              .withEnchantment(enchantment(Enchantments.MENDING), exactly(1f))
                              .withEnchantment(enchantment(Enchantments.SMITE), exactly(5f))
                        }
                    )
                )
            )
        }
    }

    override fun getName(): String = "GrowssethEntityLootTable"

    private fun enchantment(key: ResourceKey<Enchantment>) = registries.get().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)
}

class ArcheologyLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.ARCHAEOLOGY) {

    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {
        output.accept(
            GrowssethLootTables.CONDUIT_RUINS_ARCHAEOLOGY,
            lootTable()
                .withPool(
                    lootPool()
                        .setRolls(exactly(1.0f))
                        .setBonusRolls(exactly(0.0f))
                        .add(lootTableItem(GrowssethItems.GROWSSETH_POTTERY_SHERD).setWeight(15))
                        .add(lootTableItem(Items.BLADE_POTTERY_SHERD))
                        .add(lootTableItem(Items.EXPLORER_POTTERY_SHERD))
                        .add(lootTableItem(Items.MOURNER_POTTERY_SHERD))
                        .add(lootTableItem(Items.PLENTY_POTTERY_SHERD))
                        .add(lootTableItem(Items.ANGLER_POTTERY_SHERD))
                        .add(lootTableItem(Items.SHELTER_POTTERY_SHERD))
                        .add(lootTableItem(Items.IRON_AXE))
                        .add(lootTableItem(Items.EMERALD).setWeight(2))
                        .add(lootTableItem(Items.COD).setWeight(2))
                        .add(lootTableItem(Items.COAL).setWeight(2))
                        .add(lootTableItem(Items.GOLD_NUGGET).setWeight(2))
                )
                .setRandomSequence(GrowssethLootTables.CONDUIT_RUINS_ARCHAEOLOGY.location())
        )
    }

    override fun getName(): String = "GrowssethArcheologyLootTable"
}

class MiscLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.EMPTY) {

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

    override fun getName(): String = "GrowssethMiscLootTable"
}

class StructureLootTableProvider(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) : SimpleFabricLootTableProvider(output, registries, LootContextParamSets.CHEST) {

    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {
        output.accept(
            GrowssethLootTables.CHEST_ABANDONED_FORGE,
            lootTable()
                .withPool(itemOrAir(
                    lootTableItem(GrowssethItems.DISC_ABBANDONATI),
                    2
                ))
                .withPool(itemOrAir(
                    lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN),
                    1
                ))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.GROWSSETH_ARMOR_TRIM))
                )
                .withPool(lootPool()
                    .setRolls(exactly(2f))
                    .add(lootTableItem(Items.BOOK).enchantWithLevels(registries.get(), between(10f, 20f)))
                )
                .withPool(lootPool()
                    .setRolls(between(1f, 3f))
                    .add(lootTableItem(Items.IRON_SWORD))
                )                
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.IRON_INGOT).count(between(10f, 20f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_ABANDONED_FORGE_SECRET,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.STONE_SWORD).withEnchantments { 
                        it.withEnchantment(enchantment(Enchantments.SMITE), between(3f, 5f))
                          .withEnchantment(enchantment(Enchantments.VANISHING_CURSE), exactly(1f))
                    })
                )
                .withPool(itemOrAir(lootTableItem(Items.WITHER_SKELETON_SKULL), 2))
        )

        output.accept(
            GrowssethLootTables.CHEST_BEEKEEPER_HOUSE_BARRELS,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.HONEY_BOTTLE).count(between(1f, 4f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.HONEYCOMB).count(between(14f, 20f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_BEEKEEPER_HOUSE_CHEST,
            lootTable()
                .withPool(itemOrAir(lootTableItem(GrowssethItems.DISC_ODI_ET_AMO), 2))
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.FLINT_AND_STEEL))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.SHEARS))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.GLASS_BOTTLE).count(between(1f, 3f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.GLASS).count(between(4f, 8f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.CANDLE).count(between(1f, 2f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.STRING).count(between(4f, 8f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_CAVE_CAMP_CHEST_HINT,
            lootTable()
                .withPool(itemOrAir(lootTableItem(GrowssethItems.DISC_SEGA_DI_NIENTE), 2))
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.MUSHROOM_STEW).count(between(1f, 2f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.BOWL).count(between(2f, 3f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.BROWN_MUSHROOM).count(between(4f, 9f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.RED_MUSHROOM).count(between(4f, 9f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.RAW_IRON).count(between(3f, 6f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.RAW_COPPER).count(between(3f, 6f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_CONDUIT_CHURCH_TREASURE,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.DISC_CACO_CACO))
                    .add(lootTableItem(GrowssethItems.DISC_PESCI_STRANI))
                    .add(lootTableItem(Items.AIR).setWeight(4))
                )
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.HEART_OF_THE_SEA)))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.TRIDENT)
                        .enchantWithLevels(registries.get(), between(25f, 35f))
                        .withEnchantments { it.withEnchantment(enchantment(Enchantments.VANISHING_CURSE), exactly(1f)) }
                        .damage(between(0.2f, 0.4f))
                    )
                )
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.DIAMOND).count(between(3f, 5f))))
                .withPool(lootPool().setRolls(exactly(2f)).add(lootTableItem(Items.GOLD_INGOT).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_SHARD).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_CRYSTALS).count(between(5f, 10f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_CONDUIT_CHURCH_LOOT,
            lootTable()
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.NAUTILUS_SHELL).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_BRICKS).count(between(3f, 6f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE).count(between(3f, 6f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_SHARD).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_CRYSTALS).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GOLD_INGOT).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.COD).count(between(5f, 10f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.BONE).count(between(5f, 10f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_CONDUIT_RUINS_LOOT,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.DISC_CACO_CACO))
                    .add(lootTableItem(GrowssethItems.DISC_PESCI_STRANI))
                    .add(lootTableItem(Items.AIR).setWeight(4))
                )
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.NAUTILUS_SHELL).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.FISHING_ROD).count(between(0f, 1f)).enchantRandomly())
                )
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_BRICKS).count(between(3f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_SHARD).count(between(3f, 6f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.SEA_LANTERN).count(between(2f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PRISMARINE_CRYSTALS).count(between(3f, 6f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.SALMON).count(between(1f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.DRIED_KELP_BLOCK).count(between(1f, 3f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_ENCHANT_TOWER_TOP,
            lootTable()
                .withPool(itemOrAir(lootTableItem(GrowssethItems.DISC_MISSIVA_NELL_OMBRA), 2))
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.DIAMOND).count(exactly(2f))))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.IRON_SWORD)
                        .enchantWithLevels(registries.get(), between(30f, 40f))
                        .damage(between(0.2f, 0.4f))
                    )
                )
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.BOOK).count(between(1f, 5f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.LAPIS_LAZULI).count(between(6f, 15f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_ENCHANT_TOWER_TOWER,
            lootTable()
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.BOOK).enchantWithLevels(registries.get(), between(20f, 30f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.EXPERIENCE_BOTTLE).count(between(0f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.BOOK).count(between(1f, 5f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.LAPIS_LAZULI).count(between(6f, 15f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_GOLEM_HOUSES_NORMAL,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.DISC_PADRE_MAMMONK))
                    .add(lootTableItem(Items.AIR).setWeight(2))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN))
                    .add(lootTableItem(Items.AIR))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.SHEARS))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.IRON_INGOT).count(exactly(18f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.PUMPKIN_SEEDS).count(between(3f, 8f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.PUMPKIN_PIE).count(between(1f, 4f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_GOLEM_HOUSES_ZOMBIE,
            lootTable()
                .withPool(itemOrAir(lootTableItem(GrowssethItems.DISC_PADRE_MAMMONK), 2))
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.SHEARS).count(between(0f, 1f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.IRON_INGOT).count(between(2f, 6f)))
                )
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.PUMPKIN_SEEDS).count(between(0f, 3f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_NOTEBLOCK_LAB_BASEMENT,
            lootTable()
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.REPEATER).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.REDSTONE_TORCH).count(between(2f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.REDSTONE).count(between(5f, 7f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.STICK).count(between(3f, 5f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.RED_WOOL).count(between(1f, 3f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.WHITE_WOOL).count(between(1f, 3f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.STRING).count(between(1f, 3f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.AMETHYST_SHARD).count(between(5f, 8f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_NOTEBLOCK_LAB_HOUSE,
            lootTable()
                .withPool(lootPool()
                    .setRolls(between(1f, 3f))
                    .add(TagEntry.expandTag(GrowssethTags.NOTEBLOCK_DISCS_TAG))
                )
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.ROTTEN_FLESH).count(between(3f, 6f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.POISONOUS_POTATO).count(between(2f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.COBWEB).count(between(3f, 6f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_NOTEBLOCK_SHIP_BARRELS,
            lootTable()
                .withPool(lootPoolSingleRoll()
                    .add(lootTableItem(Items.POTION).count(between(0f, 10f)).potion(Potions.WATER))
                    .add(lootTableItem(Items.BREAD).count(between(0f, 10f)))
                    .add(lootTableItem(Items.COOKED_BEEF).count(between(0f, 10f)))
                    .add(lootTableItem(Items.COOKED_PORKCHOP).count(between(0f, 10f)))
                )
        )

        output.accept(
            GrowssethLootTables.CHEST_NOTEBLOCK_SHIP_CHEST,
            lootTable()
                .withPool(lootPool()
                    .setRolls(between(2f, 3f))
                    .add(TagEntry.expandTag(GrowssethTags.NOTEBLOCK_DISCS_TAG))
                )
                .withPool(itemOrAir(lootTableItem(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN), 1))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.REDSTONE).count(between(10f, 20f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.MAP).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.COMPASS)))
        )

        output.accept(
            GrowssethLootTables.CHEST_RESEARCHER_TENT_LAB,
            lootTable()
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GLASS_BOTTLE).count(exactly(3f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GOLDEN_CARROT)))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GLISTERING_MELON_SLICE)))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.SPIDER_EYE)))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GUNPOWDER).count(between(2f, 4f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.REDSTONE).count(between(1f, 2f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.GLOWSTONE_DUST).count(between(1f, 2f))))
        )

        output.accept(
            GrowssethLootTables.CHEST_RESEARCHER_TENT_TENT,
            lootTable()
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.MAP).count(between(0f, 1f))))
                .withPool(lootPoolSingleRoll().add(lootTableItem(Items.PAPER).count(between(2f, 6f))))
        )
    }

    override fun getName(): String = "GrowssethStructureLootTable"

    private fun enchantment(key: ResourceKey<Enchantment>) = registries.get().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)
}

private fun itemOrAir(item: LootPoolSingletonContainer.Builder<*>, airWeight: Int, poolRolls: Float = 1f)
    = lootPool()
        .setRolls(exactly(poolRolls))
        .add(item)
        .add(lootTableItem(Items.AIR).setWeight(airWeight))

private fun lootPoolSingleRoll() = lootPool().setRolls(exactly(1f))

private fun LootPoolSingletonContainer.Builder<*>.withEnchantments(act: (SetEnchantmentsFunction.Builder) -> SetEnchantmentsFunction.Builder): LootPoolSingletonContainer.Builder<*>
    = apply(act(SetEnchantmentsFunction.Builder()))

private fun LootPoolSingletonContainer.Builder<*>.count(countProvider: NumberProvider): LootPoolSingletonContainer.Builder<*>
    = apply(SetItemCountFunction.setCount(countProvider))

private fun LootPoolSingletonContainer.Builder<*>.potion(potion: Holder<Potion>): LootPoolSingletonContainer.Builder<*>
    = apply(SetPotionFunction.setPotion(potion))

private fun LootPoolSingletonContainer.Builder<*>.enchantWithLevels(registries: HolderLookup.Provider, levels: NumberProvider): LootPoolSingletonContainer.Builder<*>
    = apply(EnchantWithLevelsFunction.enchantWithLevels(registries, levels))

private fun LootPoolSingletonContainer.Builder<*>.enchantRandomly(): LootPoolSingletonContainer.Builder<*>
    = apply(EnchantRandomlyFunction.randomEnchantment())

private fun LootPoolSingletonContainer.Builder<*>.damage(damageProvider: NumberProvider): LootPoolSingletonContainer.Builder<*>
    = apply(SetItemDamageFunction.setDamage(damageProvider))
