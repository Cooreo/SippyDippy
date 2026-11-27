package com.cooreo.sippy

import org.junit.Assert.assertEquals
import org.junit.Test

/** Placeholder so `testDebugUnitTest` has something to run on the skeleton. */
class SmokeTest {
    @Test
    fun buildConfigVersionIsReadable() {
        // BuildConfig is generated per variant; this also proves the version
        // wiring from version.properties works.
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
    }
}
