package com.cooreo.sippy.domain.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The four ways a friend can enter the app (round 8, B8). */
enum class Vignette {
    /** Leap out of the water, spin, land on the logo with a squash. */
    LEAP,

    /** Ride a wave line across the screen, tilting with it. */
    WAVE,

    /** Float up inside a bubble that pops to reveal the logo. */
    BUBBLE,

    /** Cannonball from the top, ripple ring, logo rising out of the ripples. */
    CANNONBALL,
    ;

    companion object {
        val ALL: List<Vignette> = entries

        /**
         * A vignette that is never the one shown last time. [seed] is the random
         * part, so the order is not a fixed cycle; [lastIndex] is what was stored
         * after the previous start.
         */
        fun pick(seed: Int, lastIndex: Int): Vignette {
            val last = ALL.getOrNull(lastIndex)
            val options = if (last == null) ALL else ALL.filter { it != last }
            val index = seed.absoluteModulo(options.size)
            return options[index]
        }

        private fun Int.absoluteModulo(size: Int): Int {
            if (size <= 0) return 0
            val mod = this % size
            return if (mod < 0) mod + size else mod
        }
    }
}

/**
 * Where a friend is, and how it is turned and scaled, at one moment of a vignette
 * (round 8, B8). Pure maths over a 0..1 timeline so all four can be unit tested
 * without a screen, and so the composable only ever reads floats inside a
 * `graphicsLayer` lambda.
 */
data class Framing(
    /** Fraction of the stage width, positive right. */
    val offsetX: Float = 0f,
    /** Fraction of the stage height, positive down. */
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    /** Additional vertical squash, 1 = round, below 1 = flattened on landing. */
    val squash: Float = 1f,
    /** Degrees, clockwise. */
    val rotation: Float = 0f,
    /** How much of the landing splash ring has been drawn, 0..1. */
    val ring: Float = 0f,
    /** How much of the bubble has been drawn, 0..1. */
    val bubble: Float = 0f,
    /** True once the logo should be visible behind the friend. */
    val logo: Boolean = true,
)

object VignetteMotion {

    fun of(vignette: Vignette, t: Float): Framing {
        val time = t.coerceIn(0f, 1f)
        return when (vignette) {
            Vignette.LEAP -> leap(time)
            Vignette.WAVE -> wave(time)
            Vignette.BUBBLE -> bubble(time)
            Vignette.CANNONBALL -> cannonball(time)
        }
    }

    /** Up from below with a full spin, then a flat landing. */
    fun leap(t: Float): Framing {
        val rise = (t / 0.62f).coerceIn(0f, 1f)
        val landed = ((t - 0.62f) / 0.38f).coerceIn(0f, 1f)
        val squash = if (landed == 0f) 1f else 1f - 0.22f * sin(landed * PI.toFloat())
        return Framing(
            offsetY = 0.55f * (1f - rise),
            scale = 0.72f + 0.28f * rise,
            squash = squash,
            rotation = 360f * rise,
            ring = if (landed > 0f) landed else 0f,
        )
    }

    /** Across the stage on a wave line, tilting with the slope. */
    fun wave(t: Float): Framing {
        val phase = t * 2f * PI.toFloat()
        return Framing(
            offsetX = -0.34f + 0.68f * t,
            offsetY = 0.10f * sin(phase),
            scale = 1f,
            rotation = 10f * cos(phase),
        )
    }

    /** Up inside a bubble that pops at 70%, revealing the logo. */
    fun bubble(t: Float): Framing {
        val rise = t.coerceIn(0f, 1f)
        val popped = ((t - 0.70f) / 0.30f).coerceIn(0f, 1f)
        return Framing(
            offsetY = 0.42f * (1f - rise),
            scale = 0.86f + 0.14f * rise + 0.10f * sin(popped * PI.toFloat()),
            rotation = -8f * sin(popped * PI.toFloat()),
            bubble = if (t < 0.70f) 1f else 1f - popped,
            logo = t >= 0.70f,
        )
    }

    /** Down from above, spinning, then a ripple ring and a rising logo. */
    fun cannonball(t: Float): Framing {
        val fall = (t / 0.55f).coerceIn(0f, 1f)
        val after = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val squash = if (fall < 1f) 1f else 1f - 0.26f * sin(after * PI.toFloat())
        return Framing(
            offsetY = -0.60f + 0.60f * fall,
            scale = 0.78f + 0.22f * fall,
            squash = squash,
            rotation = 520f * fall,
            ring = if (fall >= 1f) after else 0f,
            logo = t >= 0.55f,
        )
    }
}
