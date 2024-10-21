package com.ruslan.growsseth.loot

import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable

class LootTableModifierNeo(private val lootTable: LootTable) : LootTableModifier {
    override fun addPool(lootPool: LootPool.Builder) {
        // Neo allows modifying loot tables directly
        lootTable.addPool(lootPool.build())
    }
}