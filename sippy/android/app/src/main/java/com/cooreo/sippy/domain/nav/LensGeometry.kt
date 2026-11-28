package com.cooreo.sippy.domain.nav

/**
 * Where the nav lens is, given where its two tabs are (round 9, 2.1).
 *
 * The old lens assumed five equal slots, which stops being true the moment a tab
 * label changes width or a sixth tab appears. This works from real measured bounds
 * instead: each tab reports its own rectangle, and the lens is the interpolation
 * between the tab it left and the tab it is heading for.
 *
 * Pure, in pixels, no Compose types beyond nothing at all — the caller converts dp
 * to px with its own density, which is what makes the density cases testable.
 */
data class LensBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val centerX: Float get() = left + width / 2f
    val right: Float get() = left + width
}

object LensGeometry {

    /** How much of a tab's width the lens covers. */
    const val WIDTH_FACTOR = 0.92f

    /** Air between the lens and the bar's inner edge, on every side. */
    const val INSET_DP = 8f

    /** The lens may stretch while it travels; this is the ceiling. */
    const val MAX_STRETCH = 1.25f

    /** Velocity, in slots per second, that reaches the stretch ceiling. */
    const val STRETCH_SPEED = 6f

    /**
     * The lens between [from] and [to] at [progress] (0 at the old tab, 1 at the new
     * one), inside a bar of [barHeightPx], with [density] to turn [INSET_DP] into px.
     *
     * The lens is [WIDTH_FACTOR] of the interpolated tab width — so a wide tab gives a
     * wide lens — stretched by travel speed up to [MAX_STRETCH], never taller than the
     * bar's inner height, and always centred on the point between the two tabs.
     */
    fun lensRect(
        from: LensBounds,
        to: LensBounds,
        progress: Float,
        velocity: Float,
        barHeightPx: Float,
        density: Float,
    ): LensBounds {
        val t = progress.coerceIn(0f, 1f)
        val inset = INSET_DP * density
        val width = widthAt(from, to, t) * WIDTH_FACTOR * stretch(velocity)
        val height = (barHeightPx - inset * 2f).coerceAtLeast(1f)
        val centre = from.centerX + (to.centerX - from.centerX) * t
        val left = centre - width / 2f
        return LensBounds(
            left = left,
            top = inset,
            width = width,
            // Height is the bar's inner height, so the lens never pokes out.
            height = height,
        )
    }

    /** Stretch from travel speed, symmetric and capped. */
    fun stretch(velocity: Float): Float {
        val crawl = (kotlin.math.abs(velocity) / STRETCH_SPEED).coerceIn(0f, 1f)
        return 1f + (MAX_STRETCH - 1f) * crawl
    }

    private fun widthAt(from: LensBounds, to: LensBounds, t: Float): Float =
        from.width + (to.width - from.width) * t

    /**
     * Clamps a lens rectangle into the bar, so a first frame before the tabs have
     * measured cannot draw the lens outside the glass.
     */
    fun clampToBar(rect: LensBounds, barWidthPx: Float, barHeightPx: Float, density: Float): LensBounds {
        val inset = INSET_DP * density
        val width = rect.width.coerceAtMost(barWidthPx - inset * 2f).coerceAtLeast(1f)
        val left = rect.left.coerceIn(inset, (barWidthPx - inset - width).coerceAtLeast(inset))
        return rect.copy(left = left, width = width, height = rect.height.coerceAtMost(barHeightPx - inset * 2f))
    }
}
