package com.cooreo.sippy.domain.friends

import kotlin.math.PI
import kotlin.math.sin

/**
 * The friends' idle body language (round 8, B9), as pure maths over a 0..1 cycle
 * so the screen only has to read one animated float inside a layer lambda.
 *
 * Everything here is a transform. The bob is a slow two-beat drift, the hop is a
 * single quick bump late in the cycle and is exactly zero outside its window, so
 * looping it never snaps, and the squeeze is a single damped flattening per tap.
 */
object IdleMotion {

    /** One full bob-hop cycle, long enough to read as idling not as a spinner. */
    const val CYCLE_MS = 6800

    private const val HOP_START = 0.86f
    private const val HOP_END = 0.96f

    /** Vertical drift in dp: two gentle bobs per cycle, +/-1.5dp. */
    fun bob(cycle: Float): Float = sin(cycle * 2.0 * PI * 2.0).toFloat() * 1.5f

    /** A quick 6dp hop near the end of the cycle; 0 everywhere else. */
    fun hop(cycle: Float): Float {
        if (cycle <= HOP_START || cycle >= HOP_END) return 0f
        val t = (cycle - HOP_START) / (HOP_END - HOP_START)
        return sin(t * PI.toFloat()) * 6f
    }

    /** 0 at rest, 1 at the middle of a tap reaction. */
    fun tapSwell(tap: Float): Float = sin(tap * PI.toFloat())

    /** Flattens by 16% and widens by 9% at the peak of a tap, as jelly would. */
    fun tapScaleX(tap: Float): Float = 1f + 0.09f * tapSwell(tap)

    fun tapScaleY(tap: Float): Float = 1f - 0.16f * tapSwell(tap)
}
