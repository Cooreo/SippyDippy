package com.cooreo.sippy

import com.cooreo.sippy.domain.intro.Vignette
import com.cooreo.sippy.domain.intro.VignetteMotion
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StartupAnimation
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.storage.StorageCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B8: the vignette order, the flight maths, and the startup setting. */
class IntroTest {

    @Test
    fun aVignetteNeverRepeatsTwiceRunning() {
        for (lastIndex in Vignette.ALL.indices) {
            var seed = 0
            while (seed < 200) {
                val picked = Vignette.pick(seed, lastIndex)
                assertFalse(
                    "seed $seed after $lastIndex repeated ${Vignette.ALL[lastIndex]}",
                    picked == Vignette.ALL[lastIndex],
                )
                assertTrue(picked in Vignette.ALL)
                seed += 1
            }
        }
    }

    @Test
    fun aFreshInstallCanSeeAnyVignette() {
        val seen = (0 until 400).map { Vignette.pick(it, -1) }.toSet()
        assertEquals(Vignette.ALL.toSet(), seen)
    }

    @Test
    fun everyVignetteStaysOnStage() {
        for (vignette in Vignette.ALL) {
            var step = 0
            while (step <= 100) {
                val t = step / 100f
                val f = VignetteMotion.of(vignette, t)
                assertTrue("$vignette x at $t", f.offsetX in -0.7f..0.7f)
                assertTrue("$vignette y at $t", f.offsetY in -0.7f..0.7f)
                assertTrue("$vignette scale at $t", f.scale in 0.6f..1.4f)
                assertTrue("$vignette squash at $t", f.squash in 0.7f..1.01f)
                assertTrue("$vignette ring at $t", f.ring in 0f..1f)
                assertTrue("$vignette bubble at $t", f.bubble in 0f..1f)
                assertTrue("$vignette rotation at $t", f.rotation.isFinite())
                step += 1
            }
        }
    }

    @Test
    fun theLeapStartsBelowAndLands() {
        assertTrue(VignetteMotion.leap(0f).offsetY > 0.4f)
        assertEquals(0f, VignetteMotion.leap(1f).offsetY, 0.01f)
        // The spin is a full turn, so it ends where it began.
        assertEquals(360f, VignetteMotion.leap(1f).rotation, 0.5f)
    }

    @Test
    fun theCannonballStartsAbove() {
        assertTrue(VignetteMotion.cannonball(0f).offsetY < -0.5f)
        assertEquals(0f, VignetteMotion.cannonball(1f).offsetY, 0.01f)
        assertTrue("no ripple after landing", VignetteMotion.cannonball(1f).ring > 0f)
    }

    @Test
    fun theBubbleHidesTheLogoUntilItPops() {
        assertFalse(VignetteMotion.bubble(0.3f).logo)
        assertTrue(VignetteMotion.bubble(0.95f).logo)
        assertTrue("the bubble should be biggest before the pop", VignetteMotion.bubble(0.2f).bubble > 0f)
        assertEquals(0f, VignetteMotion.bubble(1f).bubble, 0.01f)
    }

    @Test
    fun theWaveCrossesTheStage() {
        assertTrue(VignetteMotion.wave(0f).offsetX < 0f)
        assertTrue(VignetteMotion.wave(1f).offsetX > 0f)
    }

    @Test
    fun theStartupSettingDefaultsToFullAndSurvivesTheStore() {
        assertEquals(StartupAnimation.FULL, Settings().startupAnimation)
        assertEquals(-1, Settings().lastIntroVignette)
        val state = StoredState(
            settings = Settings(
                startupAnimation = StartupAnimation.OFF,
                lastIntroVignette = 2,
            ),
        )
        val decoded = StorageCodec.decode(StorageCodec.encode(state))
        assertTrue("round trip failed: $decoded", decoded is StorageCodec.Decoded.Ok)
        val back = (decoded as StorageCodec.Decoded.Ok).state.settings
        assertEquals(StartupAnimation.OFF, back.startupAnimation)
        assertEquals(2, back.lastIntroVignette)
    }
}
