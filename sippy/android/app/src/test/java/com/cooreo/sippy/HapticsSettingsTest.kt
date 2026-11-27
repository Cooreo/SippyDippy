package com.cooreo.sippy

import com.cooreo.sippy.storage.StorageCodec
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StoredState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B10: haptics are a setting, on by default, and they survive the store. */
class HapticsSettingsTest {

    @Test
    fun hapticsAreOnByDefault() {
        assertTrue(Settings().hapticsEnabled)
    }

    @Test
    fun aDocumentWithoutTheFieldStillReads() {
        // A save written by 1.2.0 has no hapticsEnabled key at all.
        val legacy = """
            {"schemaVersion":1,"settings":{"goalMl":2000,"reduceEffects":false,
            "displayName":"Maja","onboardingComplete":true},"log" : []}
        """.trimIndent()
        val decoded = StorageCodec.decode(legacy)
        assertTrue("legacy save must not be corrupt: $decoded", decoded is StorageCodec.Decoded.Ok)
        val state = (decoded as StorageCodec.Decoded.Ok).state
        assertEquals("Maja", state.settings.displayName)
        assertTrue("missing key falls back to on", state.settings.hapticsEnabled)
    }

    @Test
    fun turningItOffSurvivesARoundTrip() {
        val state = StoredState(settings = Settings(hapticsEnabled = false))
        val again = StorageCodec.decode(StorageCodec.encode(state))
        assertTrue(again is StorageCodec.Decoded.Ok)
        assertFalse((again as StorageCodec.Decoded.Ok).state.settings.hapticsEnabled)
    }
}
