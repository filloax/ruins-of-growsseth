package com.ruslan.growsseth

import com.ruslan.growsseth.utils.resLoc

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.storage.loot.LootTable

object GrowssethLootTables {
    val CONDUIT_RUINS_ARCHAEOLOGY = resKey("conduit_ruins_archaeology")

    val CHEST_ABANDONED_FORGE = resKey("chests/abandoned_forge/loot")
    val CHEST_ABANDONED_FORGE_SECRET = resKey("chests/abandoned_forge/secret")
    val CHEST_BEEKEEPER_HOUSE_BARRELS = resKey("chests/beekeeper_house/barrels")
    val CHEST_BEEKEEPER_HOUSE_CHEST = resKey("chests/beekeeper_house/chest")
    val CHEST_CAVE_CAMP_CHEST_HINT = resKey("chests/cave_camp/chest_hint")
    val CHEST_CONDUIT_CHURCH_TREASURE = resKey("chests/conduit_church/hidden_treasure")
    val CHEST_CONDUIT_CHURCH_LOOT = resKey("chests/conduit_church/loot")
    val CHEST_CONDUIT_RUINS_LOOT = resKey("chests/conduit_ruins/loot")
    val CHEST_ENCHANT_TOWER_TOP = resKey("chests/enchant_tower/top")
    val CHEST_ENCHANT_TOWER_TOWER = resKey("chests/enchant_tower/tower")
    val CHEST_GOLEM_HOUSES_NORMAL = resKey("chests/golem_houses/normal")
    val CHEST_GOLEM_HOUSES_ZOMBIE = resKey("chests/golem_houses/zombie")
    val CHEST_NOTEBLOCK_LAB_BASEMENT = resKey("chests/noteblock_lab/basement")
    val CHEST_NOTEBLOCK_LAB_HOUSE = resKey("chests/noteblock_lab/house")
    val CHEST_NOTEBLOCK_SHIP_BARRELS = resKey("chests/noteblock_ship/barrels")
    val CHEST_NOTEBLOCK_SHIP_CHEST = resKey("chests/noteblock_ship/chest")
    val CHEST_RESEARCHER_TENT_LAB = resKey("chests/researcher_tent/lab")
    val CHEST_RESEARCHER_TENT_TENT = resKey("chests/researcher_tent/tent")

    // Mod compat
    val COBBLEMON_DEFEAT_RESEARCHER = resKey("cobblemon/defeat_researcher")

    private fun resKey(path: String): ResourceKey<LootTable> = ResourceKey.create(Registries.LOOT_TABLE, resLoc(path))
}