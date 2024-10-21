package com.ruslan.growsseth.loot

import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable

interface LootTableModifier {
    fun addPool(lootPool: LootPool.Builder)

    class ForLootTableBuilder(private val builder: LootTable.Builder) : LootTableModifier {
        override fun addPool(lootPool: LootPool.Builder) {
            builder.withPool(lootPool)
        }
    }

    // Neo allows modifying loot tables directly, add method there
}