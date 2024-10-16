package com.ruslan.growsseth.loot

import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.item.GrowssethItems.DISC_ABBANDONATI
import com.ruslan.growsseth.item.GrowssethItems.DISC_BALLATA_DEL_RESPAWN
import com.ruslan.growsseth.item.GrowssethItems.DISC_MISSIVA_NELL_OMBRA
import com.ruslan.growsseth.item.GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_ARMOR_TRIM
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_BANNER_PATTERN
import com.ruslan.growsseth.item.GrowssethItems.GROWSSETH_POTTERY_SHERD
import com.ruslan.growsseth.item.GrowssethItems.RESEARCHER_DAGGER
import com.ruslan.growsseth.item.GrowssethItems.RESEARCHER_HORN
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.storage.loot.BuiltInLootTables
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
    private val END_CITIES_LOOT = BuiltInLootTables.END_CITY_TREASURE

    private val SKULK_DISCS = setOf(
        DISC_ABBANDONATI,
        DISC_MISSIVA_NELL_OMBRA,
    )

    private fun getDiscs() = GrowssethItems.all.values
        .filter { it.components().has(DataComponents.JUKEBOX_PLAYABLE) }
        .minus(SKULK_DISCS)
        .minus(DISC_BALLATA_DEL_RESPAWN)    // only fragments can be found

    fun onModifyLootTables(id: ResourceKey<LootTable>, tableBuilder: LootTable.Builder, registries: HolderLookup.Provider) {
        if (com.ruslan.growsseth.config.MiscConfig.modLootInVanillaStructures) {
            val poolBuilder = LootPool.lootPool()

            if (STRONGHOLD_LOOT == id) {
                getDiscs().forEach { poolBuilder.add(LootItem.lootTableItem(it)) }
                poolBuilder
                    .add(LootItem.lootTableItem(RESEARCHER_DAGGER))
                    .add(LootItem.lootTableItem(RESEARCHER_HORN))
            }
            else if (RUINED_PORTAL_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_BANNER_PATTERN))
            }
            else if (ANCIENT_CITY_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_ARMOR_TRIM))
                SKULK_DISCS.forEach { poolBuilder.add(LootItem.lootTableItem(it)) }
            }
            else if (DUNGEON_LOOT == id || MANSION_LOOT == id) {
                getDiscs().forEach {
                    poolBuilder.add(LootItem.lootTableItem(it))
                }
            }
            else if (OCEAN_RUIN_COLD_LOOT == id || OCEAN_RUIN_WARM_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(GROWSSETH_POTTERY_SHERD))
            }
            else if (END_CITIES_LOOT == id) {
                poolBuilder
                    .add(LootItem.lootTableItem(FRAGMENT_BALLATA_DEL_RESPAWN))
            }

            tableBuilder.withPool(poolBuilder)
        }
    }
}