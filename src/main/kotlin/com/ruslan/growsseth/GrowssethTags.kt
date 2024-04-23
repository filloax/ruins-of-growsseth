package com.ruslan.growsseth

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Instrument
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.structure.Structure

object GrowssethTags {
    // datagen (run runDatagenClient to update)
    val TENT_MATERIALS_WHITELIST: TagKey<Block> = TagKey.create(Registries.BLOCK, resLoc("tent_clearable"))
    val TENT_CLEAR_ZOMBIE_STAGE_WHITELIST: TagKey<Block> = TagKey.create(Registries.BLOCK, resLoc("tent_zombie_stage_clearable"))
    val RESEARCHER_MESS_TRIGGER: TagKey<Block> = TagKey.create(Registries.BLOCK, resLoc("researcher_mess_trigger"))
    val RESEARCHER_HORNS: TagKey<Instrument> = TagKey.create(Registries.INSTRUMENT, resLoc("researcher_horns"))

    //json
    object StructTags {
        val ALL = mutableListOf<TagKey<Structure>>()

        val RESEARCHER_TENT: TagKey<Structure> = create("researcher_tent")
        val BEEKEEPER_HOUSE: TagKey<Structure> = create("beekeeper_house")
        val CAVE_CAMP: TagKey<Structure> = create("cave_camp")
        val MARKER: TagKey<Structure> = create("marker")
        val CONDUIT_RUINS: TagKey<Structure> = create("conduit_ruins")
        val CONDUIT_CHURCH: TagKey<Structure> = create("conduit_ruins")
        val ENCHANT_TOWER: TagKey<Structure> = create("enchant_tower")
        val ABANDONED_FORGE: TagKey<Structure> = create("abandoned_forge")
        val GOLEM_HOUSE: TagKey<Structure> = create("golem_house")
        val NOTEBLOCK_LAB: TagKey<Structure> = create("noteblock_lab")
        val NOTEBLOCK_SHIP: TagKey<Structure> = create("noteblock_ship")

        private fun create(name: String): TagKey<Structure> = TagKey.create(Registries.STRUCTURE, resLoc(name)).also {
            ALL.add(it)
        }
    }
}