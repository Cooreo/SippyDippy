package com.cooreo.sippy

import com.cooreo.sippy.ui.system.TiltFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B11: the parallax filter is smooth, bounded and settles. */
class TiltTest {

    @Test
    fun degreesAreClampedToTheLeaningLimit() {
        assertEquals(0f, TiltFilter.degrees(0f), 0f)
        assertEquals(TiltFilter.MAX_DEGREES, TiltFilter.degrees(4f), 0f)
        assertEquals(-TiltFilter.MAX_DEGREES, TiltFilter.degrees(-4f), 0f)
        assertTrue(TiltFilter.degrees(0.5f) in 0f..TiltFilter.MAX_DEGREES)
    }

    @Test
    fun theFilterSettlesOnTheTargetWithoutOvershooting() {
        var value = 0f
        val target = TiltFilter.MAX_DEGREES
        repeat(400) { value = TiltFilter.next(value, target) }
        assertEquals(target, value, 0.01f)
        // Every step moves toward the target and never past it.
        var again = 0f
        repeat(200) {
            val next = TiltFilter.next(again, target)
            assertTrue("overshoot at step $it", next in again..target)
            assertTrue("jumped", next - again <= target * TiltFilter.RESPONSE + 0.0001f)
            again = next
        }
    }

    @Test
    fun aSingleJoltBarelyMovesIt() {
        val first = TiltFilter.next(0f, TiltFilter.MAX_DEGREES)
        assertTrue(first < TiltFilter.MAX_DEGREES * 0.1f)
    }
}
