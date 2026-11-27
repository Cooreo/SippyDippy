package com.cooreo.sippy.domain.layout

/**
 * How big the water orb may be once the two pinned-friend slots claim their
 * width (round 7, B2). Pure float maths so 360dp and 412dp are unit tested
 * instead of guessed: the orb scales down and both slots stay on screen.
 */
object OrbLayout {

    const val MAX_DIAMETER_DP = 248f
    const val MIN_DIAMETER_DP = 132f

    /** One friend slot beside the orb. */
    const val SLOT_DP = 56f

    /** Space between a slot and the orb. */
    const val GAP_DP = 8f

    /**
     * @param availableWidthDp the content width the row can use
     * @param pinnedCount 0, 1 or 2 — two slots always cost the same width, so a
     *   single pinned friend does not make the orb jump between sizes
     */
    fun orbDiameter(availableWidthDp: Float, pinnedCount: Int): Float {
        val slots = if (pinnedCount <= 0) 0f else (SLOT_DP + GAP_DP) * 2f
        return (availableWidthDp - slots).coerceIn(MIN_DIAMETER_DP, MAX_DIAMETER_DP)
    }

    /** Width the row needs for a given orb and slot count — used by tests. */
    fun rowWidth(orbDiameterDp: Float, pinnedCount: Int): Float {
        val slots = if (pinnedCount <= 0) 0f else (SLOT_DP + GAP_DP) * 2f
        return orbDiameterDp + slots
    }
}
