package com.ruslan.growsseth.utils

import java.io.File
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Used to encrypt (in datagen) or decrypt asset files,
 * used in the mod for the monetized music at the artists'
 * wishes. See [EncryptedMusicResourceListener] in the client
 * module for an explanation.
 */
object DecryptUtil {
    // Of course used only once
    fun generateKey(password: String): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
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

    fun writeKey(key: SecretKey, file: File) {
        file.outputStream().use { stream ->
            ObjectOutputStream(stream).writeObject(key)
        }
    }

    fun readKey(file: File): SecretKey {
        return readKey(file.inputStream())
    }

    fun readKey(inputStream: InputStream): SecretKey {
        return inputStream.use { stream ->
            ObjectInputStream(stream).readObject() as SecretKey
        }
    }
}