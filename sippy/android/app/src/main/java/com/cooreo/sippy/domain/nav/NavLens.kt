package com.cooreo.sippy.domain.nav

import kotlin.math.abs

/**
 * The maths of the liquid tab bar (round 8, B6), kept pure so the spring, the
 * snap and the stretch can be tested without a screen.
 *
 * The bar is five equal slots: four tabs with the Log button in the middle. The
 * lens travels the whole 0..4 range continuously, which is what lets it slide
 * behind the button instead of jumping over it, but it only ever settles on a tab.
 */
object NavLens {

    const val SLOTS = 5
    const val LOG_SLOT = 2

    /** The slots a lens may come to rest in, left to right. */
    val TAB_SLOTS = listOf(0, 1, 3, 4)

    /** Keeps a continuous position inside the bar. */
    fun clamp(position: Float): Float = position.coerceIn(0f, (SLOTS - 1).toFloat())

    /**
     * The tab slot nearest to [position]. Ties go left, so a release dead centre
     * is deterministic and never lands on the Log button's own slot.
     */
    fun nearestTabSlot(position: Float): Int {
        val target = clamp(position)
        var best = TAB_SLOTS.first()
        var bestDistance = Float.MAX_VALUE
        for (slot in TAB_SLOTS) {
            val distance = abs(slot - target)
            if (distance < bestDistance) {
                bestDistance = distance
                best = slot
            }
        }
        return best
    }

    /**
     * Horizontal stretch for a lens moving at [slotsPerSecond]: up to 1.25x wide,
     * which is the cap the round asks for, and it eases back to 1 as it settles.
     */
    fun stretchX(slotsPerSecond: Float): Float =
        1f + (abs(slotsPerSecond) * 0.0016f).coerceIn(0f, 0.25f)

    /** The vertical partner: a stretched droplet gets thinner, never below 0.9x. */
    fun stretchY(slotsPerSecond: Float): Float =
        1f - (abs(slotsPerSecond) * 0.0009f).coerceIn(0f, 0.10f)

    /** 1 under the lens, 0 a full slot away: drives the magnified glyph. */
    fun proximity(position: Float, slot: Int): Float =
        (1f - abs(position - slot)).coerceIn(0f, 1f)

    /** Where a slot's centre sits in pixels, for a bar [slotWidthPx] wide. */
    fun centerPx(slotPosition: Float, slotWidthPx: Float): Float =
        (clamp(slotPosition) + 0.5f) * slotWidthPx
}
