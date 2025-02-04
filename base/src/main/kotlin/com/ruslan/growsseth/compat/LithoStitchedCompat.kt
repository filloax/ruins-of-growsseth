package com.ruslan.growsseth.compat

import dev.worldgen.lithostitched.worldgen.structure.DelegatingStructure
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureType

/**
 * Only use if LithoStitched mod is enabled
 */
object LithoStitchedCompat {
    /**
     * Some mods that use LithoStitched library replaced e.g. villages with DelegatingStructure
     * (a type added by the mod), which would break some of our checks.
     */
    fun isValidJigsawCheckStructure(struct: Structure): Boolean {
        if (struct is DelegatingStructure) {
            return struct.delegate().type() == StructureType.JIGSAW
        }
        return false
    }
}