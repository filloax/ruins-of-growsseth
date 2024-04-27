package com.ruslan.growsseth.loot

import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.MiscConfig
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.item.GrowssethItems.DISC_ABBANDONATI
import com.ruslan.growsseth.item.GrowssethItems.DISC_MISSIVA_NELL_OMBRA
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_ARMOR_TRIM
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_BANNER_PATTERN
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_POTTERY_SHERD
import com.ruslan.growsseth.item.GrowssethItems.RESEARCHER_DAGGER
import com.ruslan.growsseth.item.GrowssethItems.RESEARCHER_HORN
import net.fabricmc.fabric.api.loot.v2.LootTableSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.item.RecordItem
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootDataManager
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem

object VanillaStructureLoot {
    private val OCEAN_RUIN_COLD_LOOT = BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY
    private val OCEAN_RUIN_WARM_LOOT = BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY
    private val RUINED_PORTAL_LOOT = BuiltInLootTables.RUINED_PORTAL
    private val ANCIENT_CITY_LOOT = BuiltInLootTables.ANCIENT_CITY
    private val STRONGHOLD_LOOT = BuiltInLootTables.STRONGHOLD_CORRIDOR
    private val DUNGEON_LOOT = BuiltInLootTables.SIMPLE_DUNGEON
    private val MANSION_LOOT = BuiltInLootTables.WOODLAND_MANSION

    private val SKULK_DISCS = setOf(
        DISC_ABBANDONATI,
        DISC_MISSIVA_NELL_OMBRA,
    )

    private fun getDiscs() = GrowssethItems.all.values
        .filterIsInstance<RecordItem>()
        .minus(SKULK_DISCS)

    fun onModifyLootTables(resourceManager: ResourceManager, lootManager: LootDataManager, id: ResourceLocation, tableBuilder: LootTable.Builder, source: LootTableSource) {
        if (MiscConfig.modLootInVanillaStructures) {
            val poolBuilder = LootPool.lootPool()
            if (STRONGHOLD_LOOT == id) {
                getDiscs().forEach { poolBuilder.add(LootItem.lootTableItem(it)) }
                poolBuilder
                    .add(LootItem.lootTableItem(RESEARCHER_DAGGER))
                    .add(LootItem.lootTableItem(RESEARCHER_HORN))
            } else if (RUINED_PORTAL_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_BANNER_PATTERN))
            } else if (ANCIENT_CITY_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_ARMOR_TRIM))
                SKULK_DISCS.forEach { poolBuilder.add(LootItem.lootTableItem(it)) }
            } else if (DUNGEON_LOOT == id || MANSION_LOOT == id) {
                getDiscs().forEach { poolBuilder.add(LootItem.lootTableItem(it)) }
            } else if (OCEAN_RUIN_COLD_LOOT == id || OCEAN_RUIN_WARM_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_POTTERY_SHERD))
            }
            tableBuilder.pool(poolBuilder.build())
        }
    }
}