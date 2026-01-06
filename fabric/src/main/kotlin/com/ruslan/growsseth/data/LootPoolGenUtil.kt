package com.ruslan.growsseth.data

import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootPool.lootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.NestedLootTable.inlineLootTable
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider

/** Used to give one or more (mutually exclusive and with the same weight) items a random chance to appear in a loot table */
fun randomChance(chanceToAppear: Float, vararg items: LootPoolSingletonContainer.Builder<*>): LootPool.Builder {
    val pool = lootPoolSingleRoll()
        .conditionally(LootItemRandomChanceCondition.randomChance(chanceToAppear).build())
    items.forEach { pool.add(it) }
    return pool
}

fun lootPoolSingleRoll(): LootPool.Builder = lootPool().setRolls(exactly(1f))

fun LootTable.Builder.extendWithLootTable(lootTable: LootTable) = this.withPool(lootPoolSingleRoll()
        .add(inlineLootTable(lootTable)))

fun LootPoolSingletonContainer.Builder<*>.withEnchantments(act: (SetEnchantmentsFunction.Builder) -> SetEnchantmentsFunction.Builder): LootPoolSingletonContainer.Builder<*>
        = apply(act(SetEnchantmentsFunction.Builder()))

fun LootPoolSingletonContainer.Builder<*>.count(countProvider: NumberProvider): LootPoolSingletonContainer.Builder<*>
        = apply(SetItemCountFunction.setCount(countProvider))

fun LootPoolSingletonContainer.Builder<*>.potion(potion: Holder<Potion>): LootPoolSingletonContainer.Builder<*>
        = apply(SetPotionFunction.setPotion(potion))

fun LootPoolSingletonContainer.Builder<*>.enchantWithLevels(registries: HolderLookup.Provider, levels: NumberProvider): LootPoolSingletonContainer.Builder<*>
        = apply(EnchantWithLevelsFunction.enchantWithLevels(registries, levels))

fun LootPoolSingletonContainer.Builder<*>.enchantRandomly(): LootPoolSingletonContainer.Builder<*>
        = apply(EnchantRandomlyFunction.randomEnchantment())

fun LootPoolSingletonContainer.Builder<*>.damage(damageProvider: NumberProvider): LootPoolSingletonContainer.Builder<*>
        = apply(SetItemDamageFunction.setDamage(damageProvider))