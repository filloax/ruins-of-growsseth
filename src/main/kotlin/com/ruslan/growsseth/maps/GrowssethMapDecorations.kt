package com.ruslan.growsseth.maps

import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.structure.GrowssethStructures
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.Structure

object GrowssethMapDecorations {
    private val allBuilder = mutableMapOf<ResourceLocation, CustomMapDecorationType>()

    val BEEKEEPER_HOUSE = make(
        "icon_beekeeper_house", "textures/map/icon_beekeeper_house.png",
        GrowssethStructures.BEEKEEPER_HOUSE,
    )
    val CAVE_CAMP = make(
        "icon_cave_camp", "textures/map/icon_cave_camp.png",
        GrowssethStructures.CAVE_CAMP,
    )
    // Autogen for variants (see below inside the init function)
    //val GOLEM_HOUSE = make(
    //"icon_golem_house", "textures/map/icon_golem_house.png",
    //GrowssethStructures.GOLEM_HOUSE,
    //)
    val ENCHANT_TOWER = make(
        "icon_enchant_tower", "textures/map/icon_enchant_tower.png",
        GrowssethStructures.ENCHANT_TOWER,
    )
    val ABANDONED_FORGE = make(
        "icon_forge", "textures/map/icon_forge.png",
        GrowssethStructures.ABANDONED_FORGE,
    )
    val CONDUIT_RUINS = make(
        "icon_conduit_ruins", "textures/map/icon_conduit_ruins.png",
        GrowssethStructures.CONDUIT_RUINS,
    )
    val NOTEBLOCK_LAB = make(
        "icon_noteblock_lab", "textures/map/icon_noteblock_lab.png",
        GrowssethStructures.NOTEBLOCK_LAB,
    )

    private fun make(name: String, iconPath: String, structure: ResourceKey<Structure>? = null, iconNum: Int = 0, iconsPerRow: Int = 1): CustomMapDecorationType {
        val decType = CustomMapDecorationType(resLoc(name), resLoc(iconPath), iconNum, iconsPerRow, structure = structure)
        allBuilder[decType.id] = decType
        return decType
    }

    fun init() {
        // Auto-add golem house stuff (could change map decorations to use tag, but whatever)
        GrowssethStructures.all.forEach {
            if (it.location().path.contains("golem_house")) {
                make("icon_${it.location().path}", "textures/map/icon_golem_house.png", it)
            }
        }
        val all: Map<ResourceLocation, CustomMapDecorationType> = allBuilder.toMap()
        all.values.forEach { CustomMapData.registerDecorationType(it) }
    }
}