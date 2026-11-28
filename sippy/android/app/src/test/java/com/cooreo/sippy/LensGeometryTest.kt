package com.cooreo.sippy

import com.cooreo.sippy.domain.nav.LensBounds
import com.cooreo.sippy.domain.nav.LensGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 9, 2.1: the lens is geometry, so it can be checked without a device. */
class LensGeometryTest {

    private fun tab(index: Int, tabWidth: Float, barHeight: Float = 200f) =
        LensBounds(left = index * tabWidth, top = 0f, width = tabWidth, height = barHeight)

    @Test
    fun theLensCoversNinetyTwoPercentOfTheTab() {
        val density = 2f
        val tab = tab(1, 100f)
        val rect = LensGeometry.lensRect(tab, tab, 0f, 0f, barHeightPx = 200f, density = density)
        assertEquals(92f, rect.width, 0.01f)
    }

    @Test
    fun theLensIsCentredBetweenTheTwoTabs() {
        val a = tab(0, 100f)
        val b = tab(2, 100f)
        val rect = LensGeometry.lensRect(a, b, 0.5f, 0f, 200f, 2f)
        // Centres at 50 and 250; halfway is 150.
        assertEquals(150f, rect.centerX, 0.01f)
    }

    @Test
    fun theLensIsAsTallAsTheBarInsideItsInsets() {
        val density = 3f
        val rect = LensGeometry.lensRect(tab(0, 100f), tab(1, 100f), 0f, 0f, 200f, density)
        assertEquals(200f - 2f * LensGeometry.INSET_DP * density, rect.height, 0.01f)
        assertEquals(LensGeometry.INSET_DP * density, rect.top, 0.01f)
    }

    @Test
    fun stretchIsSymmetricAndCappedAtOneAndAQuarter() {
        assertEquals(1f, LensGeometry.stretch(0f), 0.001f)
        assertEquals(LensGeometry.stretch(3f), LensGeometry.stretch(-3f), 0.0001f)
        assertEquals(1.25f, LensGeometry.stretch(6f), 0.001f)
        assertEquals(1.25f, LensGeometry.stretch(60f), 0.001f)
        assertEquals(1.25f, LensGeometry.stretch(-60f), 0.001f)
    }

    @Test
    fun aWiderTabGivesAWiderLens() {
        val narrow = tab(0, 80f)
        val wide = tab(1, 120f)
        val startW = LensGeometry.lensRect(narrow, wide, 0f, 0f, 200f, 2f).width
        val endW = LensGeometry.lensRect(narrow, wide, 1f, 0f, 200f, 2f).width
        assertTrue("width must grow with the tab: $startW -> $endW", endW > startW)
    }

    @Test
    fun realPhoneWidthsKeepTheLensInsideTheBar() {
        // 360 / 412 / 480dp at densities 2.0 to 3.5, four tabs.
        for (widthDp in listOf(360, 412, 480)) {
            for (density in listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f)) {
                val barW = widthDp * density
                val tabW = barW / 4f
                val height = 56f * density
                for (t in listOf(0f, 0.5f, 1f)) {
                    val raw = LensGeometry.lensRect(tab(0, tabW), tab(3, tabW), t, 6f, height, density)
                    val rect = LensGeometry.clampToBar(raw, barW, height, density)
                    val where = "${widthDp}dp @$density t=$t"
                    assertTrue("$where left ${rect.left} < 0", rect.left >= 0f)
                    assertTrue("$where right ${rect.right} > $barW", rect.right <= barW)
                    assertTrue("$where too wide", rect.width <= barW)
                    assertTrue("$where too tall", rect.height <= height)
                }
            }
        }
    }

    @Test
    fun progressIsClampedSoAFlingCannotOvershoot() {
        val a = tab(0, 100f)
        val b = tab(1, 100f)
        val under = LensGeometry.lensRect(a, b, -0.4f, 0f, 200f, 2f)
        val over = LensGeometry.lensRect(a, b, 1.4f, 0f, 200f, 2f)
        assertEquals(a.centerX, under.centerX, 0.01f)
        assertEquals(b.centerX, over.centerX, 0.01f)
    }
}
