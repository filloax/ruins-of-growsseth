package com.ruslan.growsseth.resource

import com.ruslan.growsseth.RuinsOfGrowsseth
import org.apache.logging.log4j.Level

// Really should be client side, but stay here to simplify token-replacement code to only work on main module
object MusicCommon {
    var hasMusicKey = false
        private set

    const val musicPw = "SupportateIlCoroDiMammonk_Spotify_DaiEGratis____ProprioDelleGrandiPalle2135342!£\"$£\"%6453$\"£\"\n\n\n\n\n\n5435636SignoreSiTantoAssai87456983754967SaiCheDiceIlSaggio342953487583"

    fun initCheck() {
        @Suppress("SENSELESS_COMPARISON")
        if (musicPw == "$" + "@MUSIC_PW@") {
            RuinsOfGrowsseth.log(Level.INFO, "Token replacement not working! Something went wrong during mod build, encrypted music won't work!")
        } else if (musicPw.isNotBlank()) {
            RuinsOfGrowsseth.log(Level.INFO, "Token replacement is working!")
            hasMusicKey = true
        } else {
            RuinsOfGrowsseth.log(Level.INFO, "Token replacement is working but no env var set! If you're a dev, did you set up build env correctly! Encrypted music won't work!")
        }
    }
}
