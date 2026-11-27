package com.cooreo.sippy

import com.cooreo.sippy.domain.nav.NavLens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B6: the lens travels, stretches within its cap, and only rests on a tab. */
class NavLensTest {

    @Test
    fun theLensStaysInsideTheBar() {
        assertEquals(0f, NavLens.clamp(-4f), 0f)
        assertEquals(4f, NavLens.clamp(9f), 0f)
        assertEquals(2.5f, NavLens.clamp(2.5f), 0f)
    }

    @Test
    fun itAlwaysSettlesOnATabAndNeverOnTheLogButton() {
        var step = -1f
        while (step <= 5f) {
            val slot = NavLens.nearestTabSlot(step)
            assertTrue("settled on $slot", slot in NavLens.TAB_SLOTS)
            assertTrue("settled on the Log slot", slot != NavLens.LOG_SLOT)
            step += 0.01f
        }
    }

    @Test
    fun deadCentreResolvesToOneTabDeterministically() {
        val first = NavLens.nearestTabSlot(2f)
        assertEquals(first, NavLens.nearestTabSlot(2f))
        assertTrue(first == 1 || first == 3)
    }

    @Test
    fun neighboursGoToTheNearerTab() {
        assertEquals(0, NavLens.nearestTabSlot(0.2f))
        assertEquals(1, NavLens.nearestTabSlot(1.2f))
        assertEquals(3, NavLens.nearestTabSlot(3.4f))
        assertEquals(4, NavLens.nearestTabSlot(4f))
    }

    @Test
    fun stretchIsCappedWhereTheRoundSays() {
        assertEquals(1f, NavLens.stretchX(0f), 0.0001f)
        assertEquals(1.25f, NavLens.stretchX(10_000f), 0.0001f)
        assertEquals(0.9f, NavLens.stretchY(10_000f), 0.0001f)
        // A slow drift stretches less than a fast flick.
        assertTrue(NavLens.stretchX(120f) < NavLens.stretchX(900f))
        // And never wider than tall in the wrong direction.
        assertTrue(NavLens.stretchY(900f) <= 1f)
    }

    @Test
    fun proximityIsATent() {
        assertEquals(1f, NavLens.proximity(1f, 1), 0f)
        assertEquals(0.5f, NavLens.proximity(1.5f, 1), 0.0001f)
        assertEquals(0f, NavLens.proximity(3f, 1), 0f)
    }

    @Test
    fun centersSitInTheMiddleOfEachSlot() {
        assertEquals(50f, NavLens.centerPx(0f, 100f), 0.001f)
        assertEquals(350f, NavLens.centerPx(3f, 100f), 0.001f)
    }
}
