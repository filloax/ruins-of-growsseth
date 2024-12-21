package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.utils.DecryptUtil
import java.io.File
import kotlin.system.exitProcess

/**
 * Takes the GROWSSETH_MUSIC_PW env user variable and creates a "sounds.key" file with it,
 * necessary for encrypting the OGGs and make the mod able to decrypt them in-game
 */
fun main() {
    val log = RuinsOfGrowsseth.LOGGER
    log.info("Creating music key...")
    MusicCommon.initCheck()
    if (!MusicCommon.hasMusicKey) {
        log.error("Cannot generate music key with no password set! Put password in environment as GROWSSETH_MUSIC_PW before building")
        exitProcess(-1)
    }
    val keyFile = File("sounds.key")
    DecryptUtil.generateRandomKeyWithPassword(MusicCommon.musicPw, keyFile)
    log.info("Generated key at ${keyFile.absolutePath}")

    val folder = File(".")
    val outFolder = folder.resolve("../base/src/main/resources/assets/growsseth/")

    keyFile.copyTo(outFolder.resolve(keyFile), overwrite = true)
    log.info("Copied key to ${outFolder.resolve(keyFile).absolutePath}")
}