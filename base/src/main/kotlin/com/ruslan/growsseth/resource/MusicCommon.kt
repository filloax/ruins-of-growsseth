package com.ruslan.growsseth.resource

import com.ruslan.growsseth.RuinsOfGrowsseth

// Really should be client side, but stay here to simplify token-replacement code to only work on main module
object MusicCommon {
    var hasMusicKey = false
        private set

    internal var musicPw = "$@MUSIC_PW@"

    fun initCheck() {
        @Suppress("SENSELESS_COMPARISON")
        if (musicPw == "$" + "@MUSIC_PW@") {
            if (System.getProperty("replaceTokens").toBoolean()) {
                RuinsOfGrowsseth.LOGGER.warn("Token replacement not working! Something went wrong during mod build, encrypted music won't work!")
            } else {
                hasMusicKey = getMusicKeyFromEnv()
            }
        } else if (musicPw.isNotBlank()) {
            RuinsOfGrowsseth.LOGGER.info("Token replacement is working!")
            hasMusicKey = true
        } else {
            RuinsOfGrowsseth.LOGGER.warn("Token replacement is working but no env var set! If you're a dev, did you set up build env correctly! Encrypted music won't work!")
        }
    }

    private fun getMusicKeyFromEnv(): Boolean {
        val growssethMusicPw = System.getenv("GROWSSETH_MUSIC_PW")
        if (growssethMusicPw != null) {
            musicPw = growssethMusicPw
            RuinsOfGrowsseth.LOGGER.info("Music key taken from user's env!")
            return true
        }
        RuinsOfGrowsseth.LOGGER.warn("Cannot get music key from user's env!")
        return false
    }
}
