package com.ruslan.growsseth.compat

interface ModCompatChecker {
    val isLithoStitchedLoaded: Boolean

    companion object {
        const val ID_LITHOSTITCHED = "lithostitched"
    }
}