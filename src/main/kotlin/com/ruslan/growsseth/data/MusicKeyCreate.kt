package com.ruslan.growsseth.data

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.utils.DecryptUtil
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream


fun main() {
    RuinsOfGrowsseth.LOGGER.info("Creating music key...")
    val key = DecryptUtil.generateKey("INSERT PASSWORD HERE; DO NOT LEAVE IN GIT")
    val keyFile = File("music.key")
    DecryptUtil.writeKey(key, keyFile)
    RuinsOfGrowsseth.LOGGER.info("Generated key at ${keyFile.absolutePath}")
}