package com.cooreo.sippy

import com.cooreo.sippy.model.AccentId
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.storage.StorageCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCodecTest {

    @Test
    fun roundTripKeepsValues() {
        val state = StoredState(settings = Settings(goalMl = 2500, accent = AccentId.TEAL))
        val decoded = StorageCodec.decode(StorageCodec.encode(state))
        assertTrue(decoded is StorageCodec.Decoded.Ok)
        val back = (decoded as StorageCodec.Decoded.Ok).state
        assertEquals(2500, back.settings.goalMl)
        assertEquals(AccentId.TEAL, back.settings.accent)
        assertEquals(StorageCodec.SCHEMA_VERSION, back.schemaVersion)
    }

    @Test
    fun blankDocumentIsACleanStart() {
        assertTrue(StorageCodec.decode(null) is StorageCodec.Decoded.Ok)
        assertTrue(StorageCodec.decode("   ") is StorageCodec.Decoded.Ok)
    }

    @Test
    fun corruptJsonIsReportedNotThrown() {
        assertTrue(StorageCodec.decode("{not json") is StorageCodec.Decoded.Corrupt)
        assertTrue(StorageCodec.decode("[1,2,3]") is StorageCodec.Decoded.Corrupt)
        assertTrue(StorageCodec.decode("{}") is StorageCodec.Decoded.Corrupt)
    }

    @Test
    fun unknownSchemaVersionIsReportedWithTheFoundNumber() {
        val decoded = StorageCodec.decode("""{"schemaVersion": 99, "settings": {}}""")
        assertTrue(decoded is StorageCodec.Decoded.UnknownVersion)
        assertEquals(99, (decoded as StorageCodec.Decoded.UnknownVersion).found)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val raw = """
            {"schemaVersion": 1, "futureField": {"a": 1},
             "settings": {"goalMl": 1750, "anotherFutureField": true}}
        """.trimIndent()
        val decoded = StorageCodec.decode(raw)
        assertTrue(decoded is StorageCodec.Decoded.Ok)
        assertEquals(1750, (decoded as StorageCodec.Decoded.Ok).state.settings.goalMl)
    }

    @Test
    fun backupFileNameIsFilesystemSafe() {
        val name = StorageCodec.backupFileName(1234L, "Unknown Version 7 / Corrupt")
        assertTrue(name.startsWith("state-1234-"))
        assertTrue(name.endsWith(".json"))
        assertTrue(name.none { it == ' ' || it == '/' })
    }
}
