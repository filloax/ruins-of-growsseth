package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.utils.DecryptUtil
import java.io.File
import kotlin.system.exitProcess

/**
 * Takes plain OGG files and encrypts them with the "sounds.key" file when the "growssethMusicEncrypt" configuration is run
 *
 * To use this, create a "music-encrypt" folder in the project's root folder and a "plain-music" folder inside it,
 * the put the OGGs there and run the configuration to get the encrypted file inside the mod's assets folder.
 *
 * Note: the "sounds.key" file must be created in advance with the "growssethMusicEncrypt" run configuration
 */
fun main() {
    val log = RuinsOfGrowsseth.LOGGER

    MusicCommon.initCheck()
    if (!MusicCommon.hasMusicKey) {
        log.error(
            "Cannot encrypt music with no password set! Put password in environment as GROWSSETH_MUSIC_PW before building"
        )
        exitProcess(-1)
    }

    val folder = File(".")      // see Fabric's build.gradle.kts file for the path (should be the project's root + /music-encrypt)
    val musFolder = folder.resolve("plain-music")
    val assetsFolder = folder.resolve("../base/src/main/resources/assets/growsseth/")
    val outFolder = assetsFolder.resolve("./soundsx")
    musFolder.mkdirs()
    outFolder.mkdirs()

    log.info("Encrypting music found in ${musFolder.absolutePath}")
    log.info("Loading key...")
    val keyFile = assetsFolder.resolve("sounds.key")        // If key changes, remember to run "growssethMusicKeyCreate" first
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