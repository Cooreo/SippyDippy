package com.cooreo.sippy

import com.cooreo.sippy.domain.layout.OrbLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The brief's two test widths. Screen padding is 20dp a side, so the row has
 * 320dp at 360dp and 372dp at 412dp.
 */
class OrbLayoutTest {

    private val at360 = 360f - 40f
    private val at412 = 412f - 40f

    @Test
    fun withNoPinnedFriendsTheOrbIsAtItsFullSize() {
        assertEquals(OrbLayout.MAX_DIAMETER_DP, OrbLayout.orbDiameter(at360, 0), 0.01f)
        assertEquals(OrbLayout.MAX_DIAMETER_DP, OrbLayout.orbDiameter(at412, 0), 0.01f)
    }

    @Test
    fun twoSlotsAt360dpKeepBothFriendsOnScreen() {
        val orb = OrbLayout.orbDiameter(at360, 2)
        assertTrue("orb shrank", orb < OrbLayout.MAX_DIAMETER_DP)
        assertTrue("row fits 360dp", OrbLayout.rowWidth(orb, 2) <= at360 + 0.01f)
        assertTrue("orb stays usable", orb >= OrbLayout.MIN_DIAMETER_DP)
    }

    @Test
    fun twoSlotsAt412dpStayOnScreen() {
        val orb = OrbLayout.orbDiameter(at412, 2)
        assertTrue(OrbLayout.rowWidth(orb, 2) <= at412 + 0.01f)
        assertEquals(244f, orb, 0.01f)
    }

    @Test
    fun theOrbNeverCollapsesBelowItsMinimum() {
        assertEquals(OrbLayout.MIN_DIAMETER_DP, OrbLayout.orbDiameter(200f, 2), 0.01f)
        assertEquals(OrbLayout.MIN_DIAMETER_DP, OrbLayout.orbDiameter(0f, 2), 0.01f)
    }

    @Test
    fun onePinnedFriendUsesTheSameSlotBudgetSoTheOrbDoesNotJump() {
        assertEquals(OrbLayout.orbDiameter(at360, 1), OrbLayout.orbDiameter(at360, 2), 0.01f)
    }
}
