package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.utils.DecryptUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.system.exitProcess


fun main() {
    val log = RuinsOfGrowsseth.LOGGER

    MusicCommon.initCheck()
    if (!MusicCommon.hasMusicKey) {
        log.error(
            "Cannot encrypt music with no password set! Put password in environment as GROWSSETH:MUSIC_PW before building"
        )
        exitProcess(-1)
    }

    val folder = File(".")
    val musFolder = folder.resolve("plain-music")
    val outFolder = folder.resolve("../../src/main/resources/assets/growsseth/soundsx")
    musFolder.mkdirs()
    outFolder.mkdirs()

    log.info("Encrypting music found in ${folder.absolutePath}")
    log.info("Loading key...")
    val keyFile = File("sounds.key")
    val key = DecryptUtil.readKey(keyFile, MusicCommon.musicPw)
    log.info("Loaded key")

    musFolder.list()?.forEach { fn ->
        val file = musFolder.resolve(fn)
        log.info("File: $file")
        val outFile = outFolder.resolve(fn.replace(".ogg", ".oggx"))
        DecryptUtil.encryptFile(key, file, outFile)
        log.info("Saved to ${outFile.absolutePath}")
    } ?: run { log.warn("No music files!") }

    log.info("Done!")
}