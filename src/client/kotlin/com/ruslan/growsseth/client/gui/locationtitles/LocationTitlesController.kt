package com.ruslan.growsseth.client.gui.locationtitles

interface LocationTitlesController {
    fun showLocationTitle(title: String)
    fun isShowingTitle(): Boolean

    companion object {
        fun get() = LocationTitlesControllerChat()
    }
}