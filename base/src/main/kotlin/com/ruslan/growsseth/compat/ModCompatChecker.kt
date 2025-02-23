package com.ruslan.growsseth.compat

interface ModCompatChecker {
    val isLithostitchedLoaded: Boolean
    val isImprovedVillagePlacementLoaded: Boolean

    companion object {
        const val ID_LITHOSTITCHED = "lithostitched"
        const val ID_IMPROVED_VILLAGE_PLACEMENT = "improved_village_placement"
    }
}