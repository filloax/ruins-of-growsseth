package com.ruslan.growsseth.utils

import java.io.*
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/**
 * Used to encrypt (in datagen) or decrypt asset files,
 * used in the mod for the monetized music at the artists'
 * wishes. See [EncryptedMusicResourceListener] in the client
 * module for an explanation.
 */
object DecryptUtil {
    fun generateRandomKeyWithPassword(password: String, file: File) {
        val randomKey = generateRandomKey()
        val ecnryptedKey = encryptKey(randomKey, password)
        writeKey(ecnryptedKey, file)
    }

    fun encryptFile(key: SecretKey, inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val inputBytes = inputFile.readBytes()
        val outputBytes = cipher.doFinal(inputBytes)
        outputFile.writeBytes(outputBytes)
    }

    fun decryptInputStream(key: SecretKey, inputStream: InputStream): CipherInputStream {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return CipherInputStream(inputStream, cipher)
    }

    fun readKey(file: File, password: String): SecretKey {
        return readKey(file.inputStream(), password)
    }

    fun readKey(inputStream: InputStream, password: String): SecretKey {
        val encryptedKey = inputStream.use { EncryptedKey.read(inputStream) }
        return decryptKey(encryptedKey, password)
    }

    // Private stuff

    private fun writeKey(encryptedKey: EncryptedKey, file: File) {
        file.outputStream().use { encryptedKey.write(it) }
    }

    private fun generateRandomKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    private fun encryptKey(key: SecretKey, password: String): EncryptedKey {
        val salt = generateRandomBytes(16)
        val iv = generateRandomBytes(12)

        val passwordKey = deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, passwordKey, gcmSpec)

        val encryptedKey = cipher.doFinal(key.encoded)

        return EncryptedKey(encryptedKey, salt, iv)
    }

    private fun decryptKey(encryptedKey: EncryptedKey, password: String): SecretKey {
        val passwordKey = deriveKeyFromPassword(password, encryptedKey.salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, encryptedKey.iv)
        cipher.init(Cipher.DECRYPT_MODE, passwordKey, gcmSpec)

        val decryptedKeyBytes = cipher.doFinal(encryptedKey.encryptedKey)
        return SecretKeySpec(decryptedKeyBytes, "AES")
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    data class EncryptedKey(val encryptedKey: ByteArray, val salt: ByteArray, val iv: ByteArray) {
        companion object {
            fun read(inputStream: InputStream): EncryptedKey {
                val arrays = readBytesRoundRobin(inputStream, 3)
                return EncryptedKey(arrays[0], arrays[1], arrays[2])
            }
        }

        fun write(outputStream: OutputStream) {
            writeBytesRoundRobin(listOf(encryptedKey, salt, iv), outputStream)
        }
    }

    // Why? I have weird hobbies, that's why
    fun writeBytesRoundRobin(arrays: List<ByteArray>, outputStream: OutputStream) {
        // Write the length of each array as a long at the start
        val outArrayLength = arrays.sumOf { it.size }
        val outArray = ByteArray(outArrayLength)

        val arrayEndings = mutableMapOf<Int, Int>()
        var arrayIndices = arrays.map{0}.toMutableList()
        var currentArray = 0
        for (i in 0 until outArrayLength) {
            var array: ByteArray
            var idx: Int
            do {
                array = arrays[currentArray]
                idx = arrayIndices[currentArray]
                if (idx >= array.size) {
                    currentArray = (currentArray + 1) % arrays.size
                }
            } while (idx >= array.size)

            outArray[i] = array[idx]
            idx++
            if (idx >= array.size) {
                arrayEndings[currentArray] = i
            }

            arrayIndices[currentArray] = idx
            currentArray = (currentArray + 1) % arrays.size
        }

        for (i in 0 until arrays.size) {
            outputStream.write(arrayEndings[i]!!)
        }

        outputStream.write(outArray)
    }

    fun readBytesRoundRobin(inputStream: InputStream, numArrays: Int): List<ByteArray> {
        // Read the lengths of each array
        val arrayEndings = mutableListOf<Int>()
        repeat(numArrays) {
            val length = inputStream.read()
            arrayEndings.add(length)
        }

        val scrambledArray = inputStream.readBytes()

        val arrays = mutableMapOf<Int, ByteArray>()
        val arrayHolders = (0 until numArrays).map{ ByteArray(scrambledArray.size) }
        val arrayDone = (0 until numArrays).associateWith { false }.toMutableMap()

        var arrayIndices = (0 until numArrays).map{0}.toMutableList()
        var currentArray = 0
        for (i in scrambledArray.indices) {
            while (arrayDone[currentArray]!!) {
                currentArray = (currentArray + 1) % numArrays
            }

            val array = arrayHolders[currentArray]
            val idx = arrayIndices[currentArray]

            array[idx] = scrambledArray[i]
            if (arrayEndings[currentArray] == i) {
                val size = idx + 1
                arrayDone[currentArray] = true
                arrays[currentArray] = ByteArray(size) { arrayHolders[currentArray][it] }
            }

            arrayIndices[currentArray]++
            currentArray = (currentArray + 1) % numArrays
        }

        if (arrays.keys.size != numArrays) {
            throw IllegalStateException("Something went wrong, got ${arrays.keys.size} arrays instead of $numArrays")
        }

        return arrays.entries.sortedBy { it.key }.map { it.value }
    }
}