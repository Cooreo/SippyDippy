package com.cooreo.sippy

import com.cooreo.sippy.ui.glass.GlassCapabilities
import com.cooreo.sippy.ui.glass.GlassDiagnostics
import com.cooreo.sippy.ui.glass.GlassTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule from round 7 A2: the system "remove animations" setting is a motion
 * preference, not a glass downgrade. An Android 15 phone must get LENS unless
 * the app's own Reduce effects switch is on.
 */
class GlassCapabilitiesTest {

    private fun decide(
        api: Int,
        reduce: Boolean = false,
        backdrop: Boolean = true,
        effects: Boolean = GlassCapabilities.renderEffectSupported(api),
        shaders: Boolean = GlassCapabilities.runtimeShaderSupported(api),
    ): GlassTier = GlassCapabilities.decide(
        apiLevel = api,
        reduceEffects = reduce,
        renderEffectSupported = effects,
        runtimeShaderSupported = shaders,
        backdropInitialised = backdrop,
    )

    private fun diagnostics(
        api: Int = 35,
        reduce: Boolean = false,
        animationsOff: Boolean = false,
        backdrop: Boolean = true,
    ) = GlassDiagnostics(
        apiLevel = api,
        reduceEffects = reduce,
        systemAnimationsOff = animationsOff,
        lowRamDevice = false,
        renderEffectSupported = GlassCapabilities.renderEffectSupported(api),
        runtimeShaderSupported = GlassCapabilities.runtimeShaderSupported(api),
        backdropInitialised = backdrop,
    )

    @Test
    fun android15WithoutReduceEffectsGetsLens() {
        assertEquals(GlassTier.LENS, decide(35))
        assertEquals(GlassTier.LENS, decide(34))
        assertEquals(GlassTier.LENS, decide(33))
    }

    @Test
    fun android12GetsBlurAndOlderGetsFrost() {
        assertEquals(GlassTier.BLUR, decide(32))
        assertEquals(GlassTier.BLUR, decide(31))
        assertEquals(GlassTier.FROST, decide(30))
    }

    @Test
    fun reduceEffectsAlwaysWinsOverTheDevice() {
        assertEquals(GlassTier.FROST, decide(35, reduce = true))
        assertEquals(GlassTier.FROST, decide(33, reduce = true))
    }

    @Test
    fun aMissingBackdropFallsBackToFrost() {
        assertEquals(GlassTier.FROST, decide(35, backdrop = false))
    }

    @Test
    fun aDeviceWithoutRuntimeShadersGetsBlurNotFlat() {
        assertEquals(GlassTier.BLUR, decide(35, shaders = false))
    }

    @Test
    fun aDeviceWithoutRenderEffectCannotBlurAtAll() {
        assertEquals(GlassTier.FROST, decide(33, effects = false))
    }

    @Test
    fun systemAnimationsOffKeepsTheLensAndOnlyStopsMotion() {
        val d = diagnostics(animationsOff = true)
        assertEquals(GlassTier.LENS, d.tier)
        assertTrue(d.motionInstant)
        assertTrue(d.motionReason.isNotBlank())
        assertTrue(d.tierReason.isNotBlank())
    }

    @Test
    fun reduceEffectsTurnsGlassOffAndStopsMotion() {
        val d = diagnostics(reduce = true)
        assertEquals(GlassTier.FROST, d.tier)
        assertTrue(d.motionInstant)
        assertTrue(d.tierReason.contains("Reduce effects"))
    }

    @Test
    fun aNormalPhoneKeepsBothGlassAndMotion() {
        val d = diagnostics()
        assertEquals(GlassTier.LENS, d.tier)
        assertFalse(d.motionInstant)
        assertTrue(d.tierReason.contains("shaders"))
    }
}
