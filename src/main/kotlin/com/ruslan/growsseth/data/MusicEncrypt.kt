package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.utils.DecryptUtil
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream


fun main() {
    val logger = RuinsOfGrowsseth.LOGGER

    val folder = File(".")
    val musFolder = folder.resolve("plain-music")
    val outFolder = folder.resolve("../../src/main/resources/assets/growsseth/soundsx")
    musFolder.mkdirs()
    outFolder.mkdirs()

    logger.info("Encrypting music found in ${folder.absolutePath}")
    logger.info("Loading key...")
    val keyFile = File("sounds.key")
    val key = DecryptUtil.readKey(keyFile)
    logger.info("Loaded key")

    musFolder.list()?.forEach { fn ->
        val file = musFolder.resolve(fn)
        logger.info("File: $file")
        val outFile = outFolder.resolve(fn.replace(".ogg", ".oggx"))
        DecryptUtil.encryptFile(key, file, outFile)
        logger.info("Saved to ${outFile.absolutePath}")
    } ?: run { logger.warn("No music files!") }

    logger.info("Done!")
}