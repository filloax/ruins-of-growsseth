package com.ruslan.growsseth.compat

abstract class ModCompatChecker {
    val isLithostitchedLoaded = isLoaded(ID_LITHOSTITCHED)
    val isImprovedVillagePlacementLoaded = isLoaded(ID_IMPROVED_VILLAGE_PLACEMENT)
    val isCobblemonLoaded = isLoaded(ID_COBBLEMON)
    val isRCTTrainerApiLoaded = isLoaded(ID_RCT_TRAINER_API)
    val isCobblemonMegaShowdownLoaded = isLoaded(ID_MEGA_SHOWDOWN)
    val isEndRemasteredLoaded = isLoaded(ID_END_REMASTERED)

    val isAllCobblemonDepsLoaded: Boolean
        get() = (isCobblemonLoaded
                && isCobblemonMegaShowdownLoaded
                && isRCTTrainerApiLoaded
                )

    abstract fun isLoaded(id: String): Boolean

    companion object {
        const val ID_LITHOSTITCHED = "lithostitched"
        const val ID_IMPROVED_VILLAGE_PLACEMENT = "improved_village_placement"
        const val ID_COBBLEMON = "cobblemon"
        const val ID_RCT_TRAINER_API = "rctapi"
        const val ID_MEGA_SHOWDOWN = "mega_showdown"
        const val ID_END_REMASTERED = "endrem"
    }
}