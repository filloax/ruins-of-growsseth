package com.ruslan.growsseth.maps

import com.filloax.fxlib.registration.RegistryHolderDelegate
import com.ruslan.growsseth.GrowssethTags.StructTags
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import javax.swing.text.html.HTML.Tag
import kotlin.jvm.optionals.getOrNull

object GrowssethMapDecorations {
    private val all = mutableMapOf<ResourceLocation, RegistryHolderDelegate<MapDecorationType>>()
    private val structureMaps = mutableMapOf<TagKey<Structure>, ResourceLocation>()

    val BEEKEEPER_HOUSE by make(
        "icon_beekeeper_house", "icon_beekeeper_house",
        StructTags.BEEKEEPER_HOUSE,
    )
    val CAVE_CAMP by make(
        "icon_cave_camp", "icon_cave_camp",
        StructTags.CAVE_CAMP,
    )
    val GOLEM_HOUSE by make(
        "icon_golem_house", "icon_golem_house",
        StructTags.GOLEM_HOUSE,
    )
    val ENCHANT_TOWER by make(
        "icon_enchant_tower", "icon_enchant_tower",
        StructTags.ENCHANT_TOWER,
    )
    val ABANDONED_FORGE by make(
        "icon_forge", "icon_forge",
        StructTags.ABANDONED_FORGE,
    )
    val CONDUIT_RUINS by make(
        "icon_conduit_ruins", "icon_conduit_ruins",
        StructTags.CONDUIT_RUINS,
    )
    val CONDUIT_CHURCH by make(
        "icon_conduit_church", "icon_conduit_ruins",
        StructTags.CONDUIT_CHURCH,
    )
    val NOTEBLOCK_LAB by make(
        "icon_noteblock_lab", "icon_noteblock_lab",
        StructTags.NOTEBLOCK_LAB,
    )

    fun getForStructure(structureTag: TagKey<Structure>): Holder<MapDecorationType>? {
        return structureMaps[structureTag]?.let { all[it]?.holder }
    }
    fun getForStructure(structureKey: ResourceKey<Structure>): Holder<MapDecorationType>? {
        val structureInfo = GrowssethStructures.info[structureKey] ?: return null
        return getForStructure(structureInfo.tag)
    }

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

        structureMaps[forStructure] = id
    }
    private fun make(name: String, path: String, forStructure: TagKey<Structure>) = make(name, decoration(resLoc(path)), forStructure)

    fun registerMapDecorations(registrator: (ResourceLocation, MapDecorationType) -> Holder<MapDecorationType>) {
        all.values.forEach{
            it.initHolder(registrator(it.id, it.value))
        }
    }
}