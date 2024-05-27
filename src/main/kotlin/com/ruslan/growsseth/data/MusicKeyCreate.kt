package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.utils.DecryptUtil
import net.minecraft.sounds.Music
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import kotlin.system.exitProcess


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
    val outFolder = folder.resolve("../../src/main/resources/assets/growsseth/")

    keyFile.copyTo(outFolder.resolve(keyFile), overwrite = true)
    log.info("Copied key to ${outFolder.resolve(keyFile).absolutePath}")
}