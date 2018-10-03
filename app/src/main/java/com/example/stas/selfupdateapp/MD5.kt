package com.example.stas.selfupdateapp

import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * @param type - algorithm name
 * MD5
 * SHA-1
 * SHA-256
 * SHA-384
 * SHA-512
 */
private fun File.calculateHash(type: String): String? {
    val hexChars = "0123456789ABCDEF"

    val digest = try {
        MessageDigest.getInstance(type)
    } catch (e: NoSuchAlgorithmException) {
        return null
    }

    return inputStream().use {
        val buffer = ByteArray(8192)
        var read: Int
        while (true) {
            read = it.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        val hashSum = digest.digest()
        val result = StringBuilder(hashSum.size * 2)
        hashSum.forEach { byte ->
            val i = byte.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        result.toString()
    }
}

val File.sha1 get() = calculateHash("SHA-1")
