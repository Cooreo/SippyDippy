package com.cooreo.sippy.domain.friends

/**
 * How grown a friend looks (round 8, B9): the streak milestone it reached turns
 * into a scale and a glow, so a 30-day friend is visibly bigger than a new one.
 * Pure, and clamped, so no caller can draw a friend larger than the layout allows.
 */
object GrowthLevel {

    /** Scale for a friend that grew at [grownAtStreak] days. 1.0 at zero. */
    fun scale(grownAtStreak: Int): Float = when {
        grownAtStreak >= 100 -> 1.18f
        grownAtStreak >= 30 -> 1.14f
        grownAtStreak >= 14 -> 1.10f
        grownAtStreak >= 7 -> 1.06f
        grownAtStreak >= 3 -> 1.03f
        else -> 1f
    }

    /** The glow the growth earns, 0f..1f — drawn as an alpha, never a blur. */
    fun glow(grownAtStreak: Int): Float = when {
        grownAtStreak >= 100 -> 0.42f
        grownAtStreak >= 30 -> 0.34f
        grownAtStreak >= 14 -> 0.26f
        grownAtStreak >= 7 -> 0.18f
        grownAtStreak >= 3 -> 0.10f
        else -> 0f
    }

    /** "grown at 14 days", or null while the friend is still small. */
    fun label(grownAtStreak: Int): String? =
        if (grownAtStreak >= 3) "grown at $grownAtStreak days" else null
}
