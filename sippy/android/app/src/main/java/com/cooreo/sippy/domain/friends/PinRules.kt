package com.cooreo.sippy.domain.friends

import com.cooreo.sippy.model.Friend

/**
 * The two slots beside the water orb (round 7, B2). Pure: the screen and the
 * controller both ask these questions, and the boundaries are unit tested.
 */
object PinRules {

    const val MAX_PINNED = 2

    fun pinnedCount(friends: List<Friend>): Int = friends.count { it.pinned }

    /** True when [id] may be pinned: already pinned, or a slot is free. */
    fun canPin(friends: List<Friend>, id: String): Boolean {
        val target = friends.firstOrNull { it.id == id } ?: return false
        return target.pinned || pinnedCount(friends) < MAX_PINNED
    }

    /**
     * Flips one friend's pin. A third pin is refused (the list comes back
     * unchanged), so the invariant "at most two pinned" cannot be broken by a
     * fast double tap.
     */
    fun toggle(friends: List<Friend>, id: String): List<Friend> {
        if (!canPin(friends, id)) return friends
        return friends.map { friend ->
            if (friend.id == id) friend.copy(pinned = !friend.pinned) else friend
        }
    }

    /** Left and right slots, in list order; null means an empty slot. */
    fun slots(friends: List<Friend>): List<Friend?> {
        val pinned = friends.filter { it.pinned }.take(MAX_PINNED)
        return listOf(pinned.getOrNull(0), pinned.getOrNull(1))
    }

    /** Drops every pin — used by the developer reset. */
    fun unpinAll(friends: List<Friend>): List<Friend> =
        friends.map { if (it.pinned) it.copy(pinned = false) else it }
}
