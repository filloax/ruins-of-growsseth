package com.ruslan.growsseth.resource

import com.ruslan.growsseth.RuinsOfGrowsseth
import org.apache.logging.log4j.Level

// Really should be client side, but stay here to simplify token-replacement code to only work on main module
object MusicCommon {
    var hasMusicKey = false
        private set

    internal var musicPw = "$@MUSIC_PW@"

    fun initCheck() {
        @Suppress("SENSELESS_COMPARISON")
        if (musicPw == "$" + "@MUSIC_PW@") {
            if (System.getProperty("replaceTokens").toBoolean()) {
                RuinsOfGrowsseth.log(Level.INFO, "Token replacement not working! Something went wrong during mod build, encrypted music won't work!")
            } else {
                hasMusicKey = getMusicKeyFromEnv()
            }
        } else if (musicPw.isNotBlank()) {
            RuinsOfGrowsseth.log(Level.INFO, "Token replacement is working!")
            hasMusicKey = true
        } else {
            RuinsOfGrowsseth.log(Level.INFO, "Token replacement is working but no env var set! If you're a dev, did you set up build env correctly! Encrypted music won't work!")
        }
    }

    private fun getMusicKeyFromEnv(): Boolean {
        val growssethMusicPw = System.getenv("GROWSSETH_MUSIC_PW")
        if (growssethMusicPw != null) {
            musicPw = growssethMusicPw
            RuinsOfGrowsseth.log(Level.INFO, "Music key taken from user's env!")
            return true
        }
        return false
    }
}
