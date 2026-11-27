package com.cooreo.sippy.util

import java.io.File
import java.security.MessageDigest

/** SHA-256 helpers used by the updater. Pure JVM, unit tested. */
object Hashing {

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.toHex()
    }

    fun sha256Hex(file: File): String = sha256Hex(file.readBytes())

    /** Constant-ish, case- and separator-insensitive comparison. */
    fun matches(expectedHex: String, actualHex: String): Boolean =
        normalize(expectedHex) == normalize(actualHex) && normalize(actualHex).isNotEmpty()

    /** Lowercase hex, no colons or spaces — the form CI compares too. */
    fun normalize(hex: String): String = hex.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }

    fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
}
