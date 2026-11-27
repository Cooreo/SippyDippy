package com.cooreo.sippy

import com.cooreo.sippy.util.Hashing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingTest {

    @Test
    fun knownSha256OfEmptyInput() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Hashing.sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun knownSha256OfAbc() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hashing.sha256Hex("abc".toByteArray(Charsets.UTF_8)),
        )
    }

    @Test
    fun matchingIgnoresCaseAndSeparators() {
        val upper = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"
        val colonised = "ba:78:16:bf:8f:01:cf:ea:41:41:40:de:5d:ae:22:23:" +
            "b0:03:61:a3:96:17:7a:9c:b4:10:ff:61:f2:00:15:ad"
        assertTrue(Hashing.matches(upper, Hashing.sha256Hex("abc".toByteArray())))
        assertTrue(Hashing.matches(colonised, Hashing.sha256Hex("abc".toByteArray())))
    }

    @Test
    fun differentHashesDoNotMatch() {
        assertFalse(Hashing.matches("deadbeef", Hashing.sha256Hex("abc".toByteArray())))
        assertFalse(Hashing.matches("", ""))
    }
}
