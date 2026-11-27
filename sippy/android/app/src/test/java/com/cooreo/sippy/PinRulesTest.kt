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

    @Test
    fun thePinLabelNamesTheSlot() {
        val a = Friend(id = "a", kind = FriendKind.CAT, name = "A")
        val b = Friend(id = "b", kind = FriendKind.KOI, name = "B")
        val c = Friend(id = "c", kind = FriendKind.OTTER, name = "C")
        val empty = listOf(a, b, c)

        assertEquals("Pin left", PinRules.pinLabel(empty, "a"))
        // Left taken: the next one is offered the right slot.
        val leftTaken = empty.map { if (it.id == "a") it.copy(pinned = true) else it }
        assertEquals("Pin right", PinRules.pinLabel(leftTaken, "b"))
        // Pinned reads Unpin, and a stranger reads the neutral Pin.
        assertEquals("Unpin", PinRules.pinLabel(leftTaken, "a"))
        assertEquals("Pin", PinRules.pinLabel(leftTaken, "nobody"))
        // Both full: neither slot is free, so nothing is promised.
        val both = leftTaken.map { if (it.id == "b") it.copy(pinned = true) else it }
        assertEquals("Pin", PinRules.pinLabel(both, "c"))
    }
}
