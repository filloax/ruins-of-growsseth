package com.ruslan.growsseth.resource

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.RuinsOfGrowsseth.log
import org.apache.logging.log4j.Level

// Really should be client side, but stay here to simplify token-replacement code to only work on main module
object MusicCommon {
    var hasMusicKey = false
        private set

    const val musicPw = "$@MUSIC_PW@"

    fun initCheck() {
        val musicKeyString = "$@HAS_MUSIC_KEY@"
        hasMusicKey = musicKeyString.toBoolean()
        @Suppress("SENSELESS_COMPARISON")
        if (musicKeyString == "$" + "@HAS_MUSIC_KEY@") {
            log(Level.INFO, "Token replacement not working! Something went wrong during mod build, encrypted music won't work!")
        } else if (hasMusicKey) {
            log(Level.INFO, "Token replacement is working!")
        } else {
            log(Level.INFO, "Token replacement is working but no env var set! If you're a dev, did you set up build env correctly! Encrypted music won't work!")
        }

        @Suppress("SENSELESS_COMPARISON")
        if (hasMusicKey && (musicPw == "$" + "@MUSIC_PW@" || musicPw.isBlank())) {
            RuinsOfGrowsseth.LOGGER.info("Token replacement works but music key not set! If you're a dev, did you set up build env correctly! Encrypted music won't work!")
            hasMusicKey = false
        }
    }
}