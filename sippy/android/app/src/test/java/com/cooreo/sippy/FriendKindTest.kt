package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.FriendNameRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 7, B3: the four new friends join the three originals. */
class FriendKindTest {

    @Test
    fun thereAreSevenKindsAndEveryLabelIsDistinct() {
        assertEquals(7, FriendKind.entries.size)
        val labels = FriendKind.entries.map { it.short }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun everyDefaultNamePassesTheNameRules() {
        FriendKind.entries.forEach { kind ->
            val name = FriendNameRules.defaultNameFor(kind)
            assertTrue("$kind → '$name'", FriendNameRules.isValid(name))
        }
    }

    @Test
    fun defaultNamesAreDistinct() {
        val names = FriendKind.entries.map { FriendNameRules.defaultNameFor(it) }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun fiveFriendsFitTheCapWithDippyPlusFour() {
        val friends = buildList {
            add(Friend.dippy())
            FriendKind.entries.filter { it != FriendKind.DIPPY }.take(4).forEachIndexed { i, kind ->
                add(Friend(id = "f$i", kind = kind, name = FriendNameRules.defaultNameFor(kind)))
            }
        }
        assertEquals(Friend.MAX_FRIENDS, friends.size)
        assertEquals(5, Friend.MAX_FRIENDS)
    }
}
