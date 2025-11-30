package com.ruslan.growsseth.compat

interface ModCompatChecker {
    val isLithostitchedLoaded: Boolean
    val isImprovedVillagePlacementLoaded: Boolean
    val isCobblemonLoaded: Boolean
    val isRCTTrainerApiLoaded: Boolean
    val isCobblemonMegaShowdownLoaded: Boolean

    val isAllCobblemonDepsLoaded: Boolean
        get() = (isCobblemonLoaded
                && isCobblemonMegaShowdownLoaded
                && isRCTTrainerApiLoaded
                )

    companion object {
        const val ID_LITHOSTITCHED = "lithostitched"
        const val ID_IMPROVED_VILLAGE_PLACEMENT = "improved_village_placement"
        const val ID_COBBLEMON = "cobblemon"
        const val ID_RCT_TRAINER_API = "rctapi"
        const val ID_MEGA_SHOWDOWN = "mega_showdown"
    }
}