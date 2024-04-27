package com.ruslan.growsseth.structure

import com.ruslan.growsseth.config.StructureConfig
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.levelgen.structure.Structure
import kotlin.jvm.optionals.getOrNull
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset

object StructureDisabler {
    object Callbacks {
        fun shouldDisableStructure(structure: Holder<Structure>, level: ServerLevel): Boolean {
            return isConfigDisabled(structure)
                    || GrowssethWorldPreset.shouldDisableStructure(structure, level)
        }
    }

    private val structToConfigMap = mapOf<ResourceKey<Structure>, () -> Boolean>(
        GrowssethStructures.RESEARCHER_TENT to StructureConfig::researcherTentEnabled,
        GrowssethStructures.CAVE_CAMP to StructureConfig::caveCampEnabled,
        GrowssethStructures.MARKER to StructureConfig::caveCampEnabled,
        GrowssethStructures.BEEKEEPER_HOUSE to StructureConfig::beekeeperHouseEnabled,
        GrowssethStructures.CONDUIT_CHURCH to StructureConfig::conduitChurchEnabled,
        GrowssethStructures.CONDUIT_RUINS to StructureConfig::conduitRuinsEnabled,
        GrowssethStructures.ENCHANT_TOWER to StructureConfig::enchantTowerEnabled,
        GrowssethStructures.ABANDONED_FORGE to StructureConfig::abandonedForgeEnabled,
        // Doesn't spawn as standalone structure naturally, but also check here
        GrowssethStructures.GOLEM_HOUSE to StructureConfig::golemHouseEnabled,
        GrowssethStructures.NOTEBLOCK_LAB to StructureConfig::noteblockLabEnabled,
        GrowssethStructures.NOTEBLOCK_SHIP to StructureConfig::noteblockShipEnabled,
    )

    private fun isConfigDisabled(structure: Holder<Structure>): Boolean {
        val id = structure.unwrapKey().getOrNull() ?: return false
        return structToConfigMap[id]?.let { !it() } ?: false
    }
}