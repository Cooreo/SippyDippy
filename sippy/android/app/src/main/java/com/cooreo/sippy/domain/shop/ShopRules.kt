package com.cooreo.sippy.domain.shop

import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.model.FriendKind

/**
 * Buying and wearing cosmetics (round 7, B4). Pure and deliberately small: the
 * controller only applies these results to the stored state.
 */
object ShopRules {

    sealed interface Purchase {
        data class Bought(
            val item: ShopItem,
            val balance: Int,
            val owned: Set<String>,
        ) : Purchase

        /** Not enough points. */
        data object TooExpensive : Purchase

        data object AlreadyOwned : Purchase

        data object UnknownItem : Purchase
    }

    fun purchase(balance: Int, owned: Set<String>, itemId: String): Purchase {
        val item = ItemTable.byId(itemId) ?: return Purchase.UnknownItem
        if (itemId in owned) return Purchase.AlreadyOwned
        val safeBalance = balance.coerceAtLeast(0)
        if (safeBalance < item.price) return Purchase.TooExpensive
        return Purchase.Bought(
            item = item,
            balance = PointsRules.debit(safeBalance, item.price),
            owned = owned + itemId,
        )
    }

    /** A friend may wear one of its own items, and only if it was bought. */
    fun canEquip(owned: Set<String>, kind: FriendKind, itemId: String): Boolean {
        val item = ItemTable.byId(itemId) ?: return false
        return itemId in owned && item.kind == kind
    }

    /**
     * Wearing [itemId] replaces whatever that friend wore before — one cosmetic
     * per friend. `null` undresses the friend. Owned items are untouched, so
     * they survive the friend they were bought for being removed.
     */
    fun equip(
        equipped: Map<String, String>,
        friendId: String,
        kind: FriendKind,
        owned: Set<String>,
        itemId: String?,
    ): Map<String, String> = when {
        itemId == null -> equipped - friendId
        canEquip(owned, kind, itemId) -> equipped + (friendId to itemId)
        else -> equipped
    }

    /** The cosmetic a friend is wearing, if it is still owned. */
    fun wornBy(equipped: Map<String, String>, owned: Set<String>, friendId: String): ShopItem? {
        val id = equipped[friendId] ?: return null
        if (id !in owned) return null
        return ItemTable.byId(id)
    }
}
