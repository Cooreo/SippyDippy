package com.cooreo.sippy

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke test for the generated BuildConfig: proves the wiring from
 * `version.properties` reaches the app without pinning a number that every
 * release would have to edit by hand.
 */
class SmokeTest {
    @Test
    fun buildConfigVersionIsReadable() {
        // BuildConfig is generated per variant, so this also proves the version
        // wiring from version.properties works. The debug variant appends
        // "-debug"; strip it and require a plain major.minor.patch number.
        val parts = BuildConfig.VERSION_NAME
            .substringBefore("-")
            .split(".")
        assertTrue(
            "VERSION_NAME should be major.minor.patch, was '${BuildConfig.VERSION_NAME}'",
            parts.size == 3 && parts.all { part -> part.isNotEmpty() && part.all(Char::isDigit) },
        )
        assertTrue("VERSION_CODE should be positive", BuildConfig.VERSION_CODE > 0)
        assertTrue(BuildConfig.APPLICATION_ID.startsWith("com.cooreo.sippy"))
    }
}
