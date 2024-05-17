package com.ruslan.growsseth.entity.researcher

import java.time.Duration
import java.time.LocalDateTime

interface ResearcherDataUser {
    val lastWorldDataTime: LocalDateTime?

    fun isUpToDateWithWorldData(savedData: ResearcherSavedData): Boolean {
        val lwdt = lastWorldDataTime ?: return true

        // positive if last update for the data was before or equal to the last update for this owner
        val diff = Duration.between(savedData.lastChangeTimestamp, lwdt)
        // Matches, or last save time was before
        // In case it's wonky enable the margin on right, but it might lead to issues when researchers save close in time
        return diff >= Duration.ZERO // Duration.ofMillis(5)
    }
}