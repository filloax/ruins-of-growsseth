//package com.ruslan.growsseth.compat.cobblemon
//
//import com.gitlab.srcmc.rctapi.api.ai.config.RCTBattleAIConfig
//import com.gitlab.srcmc.rctapi.api.util.JTO
//
//data class ResearcherRCTBattleAIConfig (
//    val moveBias: Double = DEFAULT_MOVE_BIAS,
//    val statusMoveBias: Double = DEFAULT_STATUS_MOVE_BIAS,
//    val switchBias: Double = DEFAULT_SWITCH_BIAS,
//    val itemBias: Double = DEFAULT_ITEM_BIAS,
//    val maxSelectMargin: Double = DEFAULT_MAX_SELECT_MARGIN
//) {
//    fun toRCTConfig(): RCTBattleAIConfig = RCTBattleAIConfig(moveBias, statusMoveBias, switchBias, itemBias, maxSelectMargin)
//
//    companion object {
//        private const val DEFAULT_MOVE_BIAS = 1.0
//        private const val DEFAULT_STATUS_MOVE_BIAS = 0.85
//        private const val DEFAULT_SWITCH_BIAS = 0.85
//        private const val DEFAULT_ITEM_BIAS = 0.85
//        private const val DEFAULT_MAX_SELECT_MARGIN = 0.25
//
//        /**
//         * Registers the json parser for this config type. Needs to be called as early as
//         * possible (e.g. in [ModCommon.init]).
//         */
//        fun register() {
//            JTO.registerParser(
//                "growsseth_researcher_rct",
//                { ResearcherRCTBattleAI(it) },
//                { ResearcherRCTBattleAIConfig() },
//                ResearcherRCTBattleAIConfig::class.java
//            )
//        }
//    }
//}
