package com.cooreo.sippy

import androidx.compose.ui.graphics.Color
import com.cooreo.sippy.ui.theme.BackdropSamples
import com.cooreo.sippy.ui.theme.DarkColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 9, 1.1: dark theme has to read dark, and every label has to be legible on
 * the worst background the app can put behind it.
 *
 * The background is the real, measured backdrop (see BackdropSamples) with the
 * scrim applied; the surface is the glass tint composited over it; the label is the
 * token. Everything below is WCAG 2.x relative luminance, because the rule the app
 * is held to is that one.
 */
class ContrastTest {

    private fun luminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** [over] at its own alpha, composited on [under]. */
    private fun over(over: Color, under: Color): Color {
        val a = over.alpha
        return Color(
            red = over.red * a + under.red * (1f - a),
            green = over.green * a + under.green * (1f - a),
            blue = over.blue * a + under.blue * (1f - a),
            alpha = 1f,
        )
    }

    private fun hex(value: Int) = Color(0xFF000000.toInt() or value)

    private val brightest = hex(BackdropSamples.BRIGHTEST_HEX)

    @Test
    fun theBackdropIsDarkOnceTheScrimIsOn() {
        assertTrue(
            "mean backdrop luminance is ${BackdropSamples.MEAN_LUMINANCE}",
            BackdropSamples.MEAN_LUMINANCE <= 0.10f,
        )
        assertTrue(
            "the brightest 5% must also have been dimmed by the scrim",
            BackdropSamples.BRIGHTEST_LUMINANCE < 0.45f,
        )
    }

    @Test
    fun cardTextPassesOnTheBrightestBackdropSample() {
        val card = over(DarkColors.glassTint, brightest)
        for ((name, token) in listOf(
            "text1" to DarkColors.text1,
            "text2" to DarkColors.text2,
            "text3" to DarkColors.text3,
        )) {
            val r = ratio(token, card)
            assertTrue("$name on a card is $r, needs 4.5", r >= 4.5)
        }
    }

    @Test
    fun chipTextPassesOnTheBrightestBackdropSample() {
        // Chips use the card tint at a slightly lower alpha, so they are brighter
        // and must still clear the same bar.
        val chip = over(DarkColors.glassTint.copy(alpha = 0.68f), brightest)
        for ((name, token) in listOf(
            "text1" to DarkColors.text1,
            "text2" to DarkColors.text2,
            "text3" to DarkColors.text3,
        )) {
            val r = ratio(token, chip)
            assertTrue("$name on a chip is $r, needs 4.5", r >= 4.5)
        }
    }

    @Test
    fun theSelectedChipIsDeepEnoughForWhiteText() {
        val selected = over(DarkColors.accentDeep, brightest)
        val r = ratio(Color.White, selected)
        assertTrue("white on the selected chip is $r, needs 4.5", r >= 4.5)
    }

    @Test
    fun accentTextIsAllowedAtLargeSizesOnly() {
        val card = over(DarkColors.glassTint, brightest)
        val r = ratio(DarkColors.accentBright, card)
        assertTrue("accentBright on a card is $r, needs 3.0 for large text", r >= 3.0)
    }

    @Test
    fun darkTokensHaveNoWhiteBasedFills() {
        // A white fill is what made dark theme read as frosted white-grey.
        assertTrue("glassTint must be a navy, not a white", DarkColors.glassTint.alpha > 0.4f)
        assertTrue(
            "glassTint is white-ish: ${DarkColors.glassTint}",
            luminance(DarkColors.glassTint) < 0.12,
        )
        assertTrue("chip rim stays a highlight, not a fill", DarkColors.glassRimStart.alpha < 0.30f)
    }
}
