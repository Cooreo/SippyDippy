package com.cooreo.sippy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Caps the font scale for decorative labels (round 9, 1.2).
 *
 * A tab label that grows with the system font scale stops being a tab label: it
 * either wraps or pushes the glyph out of its slot. Decorative text caps at 1.3 and
 * everything else scales fully, with its container growing instead.
 */
@Composable
fun CappedTextScale(
    max: Float = 1.3f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    // Density is an interface; the factory function is the way to derive one.
    val capped = Density(
        density = density.density,
        fontScale = cappedFontScale(density.fontScale, max),
    )
    CompositionLocalProvider(LocalDensity provides capped) { content() }
}

/** The same cap, as a plain function, so the rule is testable without a screen. */
fun cappedFontScale(current: Float, max: Float = 1.3f): Float = current.coerceAtMost(max)
