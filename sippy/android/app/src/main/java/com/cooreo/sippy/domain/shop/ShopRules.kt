package com.cooreo.sippy.domain.shop

import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.model.FriendKind

/**
 * What each friend is wearing, one item per slot (round 8, A4).
 *
 * Two maps keyed by friend id rather than a list of "outfits": renaming a friend
 * keeps the id, so it keeps its clothes. Nothing here touches [owned] except to
 * refuse an item that was never bought.
 */
data class Wardrobe(
    val hats: Map<String, String> = emptyMap(),
    val outfits: Map<String, String> = emptyMap(),
) {
    fun on(slot: ItemSlot, friendId: String): String? = when (slot) {
        ItemSlot.HAT -> hats[friendId]
        ItemSlot.OUTFIT -> outfits[friendId]
    }

    fun with(friendId: String, item: ShopItem): Wardrobe = when (item.slot) {
        ItemSlot.HAT -> copy(hats = hats + (friendId to item.id))
        ItemSlot.OUTFIT -> copy(outfits = outfits + (friendId to item.id))
    }

    fun without(friendId: String, slot: ItemSlot): Wardrobe = when (slot) {
        ItemSlot.HAT -> copy(hats = hats - friendId)
        ItemSlot.OUTFIT -> copy(outfits = outfits - friendId)
    }
}

/**
 * Buying and wearing cosmetics (round 7's shop, extended in round 8, A4). Pure and
 * deliberately small: the controller only applies these results to stored state.
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
     * The two things a friend is wearing, resolved against [owned] and against the
     * item's real slot. A value left in the hat map by an older document is
     * promoted to the outfit slot if that is what it actually is, so pre-A4 saves
     * keep their scarf.
     */
    fun worn(
        hats: Map<String, String>,
        outfits: Map<String, String>,
        owned: Set<String>,
        friendId: String,
    ): List<ShopItem> {
        val wornItems = mutableListOf<ShopItem>()
        val hatish = hats[friendId]?.let(ItemTable::byId)
        when (hatish?.slot) {
            ItemSlot.HAT -> if (hatish.id in owned) wornItems += hatish
            ItemSlot.OUTFIT -> if (hatish.id in owned) wornItems += hatish
            null -> Unit
        }
        outfits[friendId]?.let(ItemTable::byId)?.let { item ->
            if (item.id in owned && item.slot == ItemSlot.OUTFIT && wornItems.none { it.id == item.id }) {
                wornItems += item
            }
        }
        return wornItems
    }

    /** The item on one slot, or null. */
    fun wornOn(
        hats: Map<String, String>,
        outfits: Map<String, String>,
        owned: Set<String>,
        friendId: String,
        slot: ItemSlot,
    ): ShopItem? = worn(hats, outfits, owned, friendId).firstOrNull { it.slot == slot }

    /**
     * Tap-to-wear: sets the item on its slot, or takes it off if it is already
     * there. Refuses an item that is not owned or belongs to another species.
     */
    fun toggle(
        wardrobe: Wardrobe,
        friendId: String,
        kind: FriendKind,
        owned: Set<String>,
        itemId: String,
    ): Wardrobe {
        val item = ItemTable.byId(itemId) ?: return wardrobe
        if (!canEquip(owned, kind, itemId)) return wardrobe
        val current = wardrobe.on(item.slot, friendId)
        return if (current == itemId) wardrobe.without(friendId, item.slot) else wardrobe.with(friendId, item)
    }

    /** Takes one slot off a friend. */
    fun unequip(wardrobe: Wardrobe, friendId: String, slot: ItemSlot): Wardrobe =
        wardrobe.without(friendId, slot)
}
