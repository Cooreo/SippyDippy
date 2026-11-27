package com.cooreo.sippy

import com.cooreo.sippy.domain.updates.ApkUrlPolicy
import com.cooreo.sippy.domain.updates.UpdateDecision
import com.cooreo.sippy.domain.updates.UpdateManifest
import com.cooreo.sippy.domain.updates.UpdateManifestParser
import com.cooreo.sippy.domain.updates.UpdatePolicy
import com.cooreo.sippy.domain.updates.VersionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestTest {

    private val goodUrl =
        "https://github.com/cooreo/SippyDippy/releases/download/v1.1.0/sippy-1.1.0.apk"

    private val manifestJson = """
        {
          "versionCode": 2,
          "versionName": "1.1.0",
          "apkUrl": "$goodUrl",
          "sha256": "abc123",
          "sizeBytes": 1234567,
          "minSdk": 26,
          "releasedAt": "2026-12-01T10:00:00Z",
          "notes": ["Fixed the ring", "Faster start"],
          "futureField": {"ignored": true}
        }
    """.trimIndent()

    @Test
    fun manifestParsesAndIgnoresUnknownFields() {
        val manifest = UpdateManifestParser.parse(manifestJson)
        assertNotNull(manifest)
        assertEquals(2, manifest!!.versionCode)
        assertEquals("1.1.0", manifest.versionName)
        assertEquals(2, manifest.notes.size)
        assertEquals(1234567L, manifest.sizeBytes)
    }

    @Test
    fun garbageManifestIsNullNotAnException() {
        assertNull(UpdateManifestParser.parse(null))
        assertNull(UpdateManifestParser.parse(""))
        assertNull(UpdateManifestParser.parse("<html>404</html>"))
    }

    @Test
    fun defaultsFillMissingFields() {
        val manifest = UpdateManifestParser.parse("""{"versionCode": 3}""")
        assertEquals("", manifest!!.versionName)
        assertEquals(26, manifest.minSdk)
        assertTrue(manifest.notes.isEmpty())
    }

    @Test
    fun onlyStrictlyNewerVersionsAreOffered() {
        assertTrue(VersionPolicy.isNewer(2, 1))
        assertFalse(VersionPolicy.isNewer(1, 1))
        assertFalse(VersionPolicy.isNewer(0, 1))
    }

    @Test
    fun apkUrlMustLiveUnderTheReleasesPrefix() {
        assertTrue(ApkUrlPolicy.isAllowed(goodUrl))
        assertFalse(ApkUrlPolicy.isAllowed("http://github.com/cooreo/SippyDippy/releases/download/v1/a.apk"))
        assertFalse(ApkUrlPolicy.isAllowed("https://evil.example/sippy.apk"))
        assertFalse(ApkUrlPolicy.isAllowed("https://github.com/cooreo/SippyDippy/releases/download/v1/a.apk\n"))
        assertFalse(ApkUrlPolicy.isAllowed(""))
    }

    private fun manifest(versionCode: Int, url: String = goodUrl, minSdk: Int = 26) = UpdateManifest(
        versionCode = versionCode,
        versionName = "9.9.9",
        apkUrl = url,
        sha256 = "abc",
        minSdk = minSdk,
    )

    @Test
    fun offersNewerBuildOnManualCheck() {
        val decision = UpdatePolicy.decide(
            manifest = manifest(5),
            currentVersionCode = 4,
            sdkInt = 34,
            snoozedUntilEpochMs = 0L,
            nowEpochMs = 1_000L,
            manual = true,
        )
        assertTrue(decision is UpdateDecision.Offer)
    }

    @Test
    fun rejectsSameOrOlderBuildAndBadUrlAndTooNewSdk() {
        fun decide(m: UpdateManifest, sdk: Int = 34) = UpdatePolicy.decide(
            manifest = m,
            currentVersionCode = 4,
            sdkInt = sdk,
            snoozedUntilEpochMs = 0L,
            nowEpochMs = 1_000L,
            manual = true,
        )
        assertEquals(UpdateDecision.NoUpdate, decide(manifest(4)))
        assertEquals(UpdateDecision.NoUpdate, decide(manifest(5, url = "https://evil.example/a.apk")))
        assertEquals(UpdateDecision.NoUpdate, decide(manifest(5, minSdk = 35), sdk = 34))
        assertEquals(UpdateDecision.NoUpdate, decide(manifest(5), sdk = 25))
    }

    @Test
    fun automaticChecksAreRateLimitedToOnceADay() {
        val now = 10 * UpdatePolicy.CHECK_INTERVAL_MS
        assertFalse(UpdatePolicy.shouldCheckAutomatically(now - 1000L, now))
        assertTrue(UpdatePolicy.shouldCheckAutomatically(now - UpdatePolicy.CHECK_INTERVAL_MS, now))
        assertTrue(UpdatePolicy.shouldCheckAutomatically(0L, now))
    }

    @Test
    fun snoozeSuppressesAutomaticOffersButNotManualOnes() {
        val now = 100_000_000L
        val snoozed = UpdatePolicy.decide(
            manifest = manifest(5),
            currentVersionCode = 4,
            sdkInt = 34,
            snoozedUntilEpochMs = now + 1,
            nowEpochMs = now,
            manual = false,
            lastCheckEpochMs = 0L,
        )
        assertEquals(UpdateDecision.Skipped, snoozed)
        val manual = UpdatePolicy.decide(
            manifest = manifest(5),
            currentVersionCode = 4,
            sdkInt = 34,
            snoozedUntilEpochMs = now + 1,
            nowEpochMs = now,
            manual = true,
        )
        assertTrue(manual is UpdateDecision.Offer)
    }
}
