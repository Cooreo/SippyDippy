package com.cooreo.sippy

import org.junit.Assert.assertTrue
import org.junit.Test

/** Placeholder so `testDebugUnitTest` has something to run on the skeleton. */
class SmokeTest {
    @Test
    fun buildConfigVersionIsReadable() {
        // BuildConfig is generated per variant; this also proves the version
        // wiring from version.properties works.
        // The debug variant appends "-debug", so assert the prefix.
        assertTrue(BuildConfig.VERSION_NAME.startsWith("1.0.0"))
        assertTrue(BuildConfig.VERSION_CODE >= 1)
        assertTrue(BuildConfig.APPLICATION_ID.startsWith("com.cooreo.sippy"))
    }
}
