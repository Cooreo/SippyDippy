package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.PinRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinRulesTest {

    private fun friend(id: String, pinned: Boolean = false) =
        Friend(id = id, kind = FriendKind.PLANT, name = id.uppercase(), pinned = pinned)

    private val dippy = friend("dippy")
    private val plant = friend("plant")
    private val cat = friend("cat")

    @Test
    fun pinningStartsEmptyAndFillsLeftThenRight() {
        val pinned = PinRules.toggle(listOf(dippy, plant, cat), "plant")
        assertEquals(1, PinRules.pinnedCount(pinned))
        val slots = PinRules.slots(pinned)
        assertEquals("plant", slots[0]?.id)
        assertNull(slots[1])
    }

    @Test
    fun twoPinsFillBothSlotsInListOrder() {
        val pinned = PinRules.toggle(PinRules.toggle(listOf(dippy, plant, cat), "cat"), "dippy")
        val slots = PinRules.slots(pinned)
        assertEquals("dippy", slots[0]?.id)
        assertEquals("cat", slots[1]?.id)
    }

    @Test
    fun aThirdPinIsRefusedAndChangesNothing() {
        val two = PinRules.toggle(PinRules.toggle(listOf(dippy, plant, cat), "dippy"), "plant")
        assertFalse(PinRules.canPin(two, "cat"))
        val refused = PinRules.toggle(two, "cat")
        assertEquals(two, refused)
        assertEquals(2, PinRules.pinnedCount(refused))
        assertFalse(refused.first { it.id == "cat" }.pinned)
    }

    @Test
    fun unpinningAlwaysWorks() {
        val two = PinRules.toggle(PinRules.toggle(listOf(dippy, plant, cat), "dippy"), "plant")
        val freed = PinRules.toggle(two, "dippy")
        assertFalse(freed.first { it.id == "dippy" }.pinned)
        assertTrue(PinRules.canPin(freed, "cat"))
    }

    @Test
    fun togglingAnUnknownFriendIsANoOp() {
        val friends = listOf(dippy, plant)
        assertEquals(friends, PinRules.toggle(friends, "nobody"))
        assertFalse(PinRules.canPin(friends, "nobody"))
    }

    @Test
    fun unpinAllClearsEverySlot() {
        val two = PinRules.toggle(PinRules.toggle(listOf(dippy, plant, cat), "dippy"), "cat")
        assertEquals(0, PinRules.pinnedCount(PinRules.unpinAll(two)))
    }
}
