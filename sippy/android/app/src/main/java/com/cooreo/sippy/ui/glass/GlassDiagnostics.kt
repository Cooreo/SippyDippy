package com.cooreo.sippy.ui.glass

/**
 * Which glass the app draws, and why.
 *
 * Every input is a measured fact supplied by the caller, so this file is pure
 * and unit tested on the JVM. The rule (round 7, A2):
 *
 *  - the app's **Reduce effects** switch is the only thing that turns the glass
 *    off (FROST: tint + rim, zero GPU work);
 *  - the system "remove animations" setting is a *motion* preference: it makes
 *    every animation instant but must never downgrade the glass, or an Android
 *    15 phone would silently render flat cards;
 *  - API 33+ with runtime shader support gets LENS (blur + refraction),
 *    API 31–32 gets BLUR, API < 31 (no RenderEffect) gets FROST.
 */
object GlassCapabilities {

    /** RenderEffect arrived with API 31; without it there is nothing to blur. */
    fun renderEffectSupported(apiLevel: Int): Boolean = apiLevel >= 31

    /** AGSL runtime shaders — what the lens refraction is built from — arrived in 33. */
    fun runtimeShaderSupported(apiLevel: Int): Boolean = apiLevel >= 33

    fun decide(
        apiLevel: Int,
        reduceEffects: Boolean,
        renderEffectSupported: Boolean,
        runtimeShaderSupported: Boolean,
        backdropInitialised: Boolean,
    ): GlassTier = when {
        reduceEffects -> GlassTier.FROST
        !backdropInitialised -> GlassTier.FROST
        !renderEffectSupported -> GlassTier.FROST
        apiLevel >= 33 && runtimeShaderSupported -> GlassTier.LENS
        else -> GlassTier.BLUR
    }
}

/**
 * A snapshot of the facts the glass depends on, shown verbatim in Developer
 * options so a "flat" screen can never be a mystery again.
 */
data class GlassDiagnostics(
    val apiLevel: Int,
    val reduceEffects: Boolean,
    val systemAnimationsOff: Boolean,
    val lowRamDevice: Boolean,
    val renderEffectSupported: Boolean,
    val runtimeShaderSupported: Boolean,
    val backdropInitialised: Boolean,
) {
    val tier: GlassTier = GlassCapabilities.decide(
        apiLevel = apiLevel,
        reduceEffects = reduceEffects,
        renderEffectSupported = renderEffectSupported,
        runtimeShaderSupported = runtimeShaderSupported,
        backdropInitialised = backdropInitialised,
    )

    /** True when every animation must be instant (Reduce effects or system preference). */
    val motionInstant: Boolean = reduceEffects || systemAnimationsOff

    val tierReason: String = when {
        reduceEffects -> "Reduce effects is on (app setting)"
        !backdropInitialised -> "backdrop layer not initialised"
        !renderEffectSupported -> "API $apiLevel has no RenderEffect (needs 31+)"
        apiLevel >= 33 && runtimeShaderSupported -> "API $apiLevel has RenderEffect + AGSL shaders"
        apiLevel >= 33 -> "API $apiLevel reports no runtime shaders"
        else -> "API $apiLevel has RenderEffect but no AGSL shaders (needs 33+)"
    }

    val motionReason: String = when {
        reduceEffects && systemAnimationsOff -> "Reduce effects + system animations off"
        reduceEffects -> "Reduce effects is on (app setting)"
        systemAnimationsOff -> "system animations off (springs are instant)"
        else -> "animations on"
    }
}
