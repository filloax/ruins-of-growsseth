//package com.ruslan.growsseth.compat
//
//import net.fabricmc.loader.api.FabricLoader
//
//class ModCompatCheckerFabric : ModCompatChecker {
//    override val isLithostitchedLoaded: Boolean by lazy {
//        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_LITHOSTITCHED)
//    }
//    override val isImprovedVillagePlacementLoaded: Boolean by lazy {
//        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_IMPROVED_VILLAGE_PLACEMENT)
//    }
//    override val isCobblemonLoaded: Boolean by lazy {
//        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_COBBLEMON)
//    }
//    override val isRCTTrainerApiLoaded: Boolean by lazy {
//        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_RCT_TRAINER_API)
//    }
//    override val isCobblemonMegaShowdownLoaded: Boolean by lazy {
//        FabricLoader.getInstance().isModLoaded(ModCompatChecker.ID_MEGA_SHOWDOWN)
//    }
//}