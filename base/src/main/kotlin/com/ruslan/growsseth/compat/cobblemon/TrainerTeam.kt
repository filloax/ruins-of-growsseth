package com.ruslan.growsseth.compat.cobblemon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TrainerTeam {
    @SerialName("researcherStandard")
    RESEARCHER_STANDARD,
    @SerialName("researcherMaxLevel")
    RESEARCHER_MAX_LEVEL,
}