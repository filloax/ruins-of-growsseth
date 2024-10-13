package com.ruslan.growsseth.client.gui.locationtitles

import com.ruslan.growsseth.config.ClientConfig
import com.ruslan.growsseth.config.TitleMode

interface LocationTitlesController {
    fun showLocationTitle(title: String)
    fun isShowingTitle(): Boolean

    companion object {
        private val instances = mutableMapOf<TitleMode, LocationTitlesController>()

        fun get() = com.ruslan.growsseth.config.ClientConfig.locationTitlesMode?.let { mode ->
            instances.computeIfAbsent(mode) {
                when(mode) {
                    TitleMode.TITLE -> LocationTitlesControllerTitle()
                    TitleMode.CHAT_OVERLAY -> LocationTitlesControllerChat()
                }
            }
        } ?: throw IllegalStateException("location titles mode null")
    }
}