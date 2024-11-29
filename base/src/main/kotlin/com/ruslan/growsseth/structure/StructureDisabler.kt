package com.ruslan.growsseth.structure

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.StructureConfig
import com.ruslan.growsseth.structure.GrowssethStructures.getStructureSetId
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.levelgen.structure.Structure
import kotlin.jvm.optionals.getOrNull
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset
import net.minecraft.world.level.biome.BiomeSource
import net.minecraft.world.level.levelgen.structure.StructureSet

object StructureDisabler {
    object Mixins {
        @JvmStatic
        fun shouldDisableStructure(structure: Holder<Structure>, level: ServerLevel): Boolean {
            return isConfigDisabled(structure)
                    || GrowssethWorldPreset.shouldDisableStructure(structure, level)
        }

        @JvmStatic
        fun filterStructureSets(possibleStructureSets: List<Holder<StructureSet>>, biomeSource: BiomeSource) = possibleStructureSets.filter { set ->
            if (GrowssethWorldPreset.shouldDisableStructureSet(set, biomeSource)) return@filter false
            val key = set.unwrapKey().getOrNull() ?: return@filter true
            structSetToConfigMap[key]?.let { it() } ?: true
        }
    }

    private val structToConfigMap = mapOf<ResourceKey<Structure>, () -> Boolean>(
        GrowssethStructures.RESEARCHER_TENT to StructureConfig::researcherTentEnabled,
        GrowssethStructures.RESEARCHER_TENT_SIMPLE to StructureConfig::researcherTentSimpleEnabled,
        GrowssethStructures.CAVE_CAMP to StructureConfig::caveCampEnabled,
        GrowssethStructures.MARKER to StructureConfig::caveCampEnabled,
        GrowssethStructures.BEEKEEPER_HOUSE to StructureConfig::beekeeperHouseEnabled,
        GrowssethStructures.CONDUIT_CHURCH to StructureConfig::conduitChurchEnabled,
        GrowssethStructures.CONDUIT_RUINS to StructureConfig::conduitRuinsEnabled,
        GrowssethStructures.ENCHANT_TOWER to StructureConfig::enchantTowerEnabled,
        GrowssethStructures.ABANDONED_FORGE to StructureConfig::abandonedForgeEnabled,
        // Doesn't spawn as standalone structure naturally
//        GrowssethStructures.GOLEM_HOUSE to StructureConfig::golemHouseEnabled,
        GrowssethStructures.NOTEBLOCK_LAB to StructureConfig::noteblockLabEnabled,
        GrowssethStructures.NOTEBLOCK_SHIP to StructureConfig::noteblockShipEnabled,
    )
    private val structSetToConfigMap = mapOf<ResourceKey<StructureSet>, () -> Boolean>(
        structSet(GrowssethStructures.RESEARCHER_TENT) to StructureConfig::researcherTentEnabled,
        structSet(GrowssethStructures.RESEARCHER_TENT_SIMPLE) to StructureConfig::researcherTentSimpleEnabled,
        structSet(GrowssethStructures.CAVE_CAMP) to StructureConfig::caveCampEnabled,
        structSet(GrowssethStructures.BEEKEEPER_HOUSE) to StructureConfig::beekeeperHouseEnabled,
        structSet(GrowssethStructures.CONDUIT_CHURCH) to StructureConfig::conduitChurchEnabled,
        structSet(GrowssethStructures.CONDUIT_RUINS) to StructureConfig::conduitRuinsEnabled,
        structSet(GrowssethStructures.ENCHANT_TOWER) to StructureConfig::enchantTowerEnabled,
        structSet(GrowssethStructures.ABANDONED_FORGE) to StructureConfig::abandonedForgeEnabled,
        structSet(GrowssethStructures.NOTEBLOCK_LAB) to StructureConfig::noteblockLabEnabled,
        structSet(GrowssethStructures.NOTEBLOCK_SHIP) to StructureConfig::noteblockShipEnabled,
    )

    private fun isConfigDisabled(structure: Holder<Structure>): Boolean {
        val id = structure.unwrapKey().getOrNull() ?: return false
        if (id.location().namespace == RuinsOfGrowsseth.MOD_ID && !structToConfigMap.containsKey(id))
            RuinsOfGrowsseth.LOGGER.warn("No enabled config available for structure $id!")
        return structToConfigMap[id]?.let { !it() } ?: false
    }

    private fun structSet(id: ResourceKey<Structure>) = getStructureSetId(id) ?: throw IllegalStateException("No structure set for structure $id")
}