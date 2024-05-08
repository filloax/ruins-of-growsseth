package com.ruslan.growsseth.item

import com.filloax.fxlib.registration.RegistryHolderDelegate
import com.ruslan.growsseth.GrowssethTags.StructTags
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType

object GrowssethMapDecorations {
    private val all = mutableMapOf<ResourceLocation, RegistryHolderDelegate<MapDecorationType>>()
    private val structureMaps = mutableMapOf<TagKey<Structure>, MutableList<ResourceLocation>>()

    val BEEKEEPER_HOUSE by make(
        "icon_beekeeper_house", "textures/map/icon_beekeeper_house.png",
        StructTags.BEEKEEPER_HOUSE,
    )
    val CAVE_CAMP by make(
        "icon_cave_camp", "textures/map/icon_cave_camp.png",
        StructTags.CAVE_CAMP,
    )
    val GOLEM_HOUSE = make(
        "icon_golem_house", "textures/map/icon_golem_house.png",
        StructTags.GOLEM_HOUSE,
    )
    val ENCHANT_TOWER by make(
        "icon_enchant_tower", "textures/map/icon_enchant_tower.png",
        StructTags.ENCHANT_TOWER,
    )
    val ABANDONED_FORGE by make(
        "icon_forge", "textures/map/icon_forge.png",
        StructTags.ABANDONED_FORGE,
    )
    val CONDUIT_RUINS by make(
        "icon_conduit_ruins", "textures/map/icon_conduit_ruins.png",
        StructTags.CONDUIT_RUINS,
    )
    val CONDUIT_CHURCH by make(
        "icon_conduit_church", "textures/map/icon_conduit_ruins.png",
        StructTags.CONDUIT_CHURCH,
    )
    val NOTEBLOCK_LAB by make(
        "icon_noteblock_lab", "textures/map/icon_noteblock_lab.png",
        StructTags.NOTEBLOCK_LAB,
    )

    private fun decoration(
        assetId: ResourceLocation,
        showOnItemFrame: Boolean = true,
        mapColor: Int = -1,
        explorationMapElement: Boolean = true,
        trackCount: Boolean = true,
    ) = MapDecorationType(assetId, showOnItemFrame, mapColor, explorationMapElement, trackCount)

    private fun make(name: String, decorationType: MapDecorationType, forStructure: TagKey<Structure>) = RegistryHolderDelegate(resLoc(name), decorationType).apply {
        if (all.containsKey(id))
            throw IllegalArgumentException("Effect $name already registered!")
        all[id] = this

        structureMaps.computeIfAbsent(forStructure) { mutableListOf() }.add(id)
    }
    private fun make(name: String, path: String, forStructure: TagKey<Structure>) = make(name, decoration(resLoc(path)), forStructure)

    fun registerMapDecorations(registrator: (ResourceLocation, MapDecorationType) -> Holder<MapDecorationType>) {
        all.values.forEach{
            it.initHolder(registrator(it.id, it.value))
        }
    }
}