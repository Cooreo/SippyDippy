package com.cooreo.sippy.domain.shop

import com.cooreo.sippy.model.FriendKind

enum class ItemSlot { HAT, OUTFIT }

/**
 * One cosmetic. Prices are the round-7 proposal — marked "proposed" in
 * PROGRESS.md, because they are a product decision, not a technical one.
 */
data class ShopItem(
    val id: String,
    val kind: FriendKind,
    val slot: ItemSlot,
    val name: String,
    val price: Int,
)

/**
 * The five cosmetics whose art exists (see sippy/android/ART_CREDITS.md). The
 * table lists nothing without art, so the shop can never point at a missing
 * drawable.
 */
object ItemTable {

    val items: List<ShopItem> = listOf(
        ShopItem("koi_hat", FriendKind.KOI, ItemSlot.HAT, "Sailor hat", 30),
        ShopItem("koi_scarf", FriendKind.KOI, ItemSlot.OUTFIT, "Kelp scarf", 45),
        ShopItem("otter_hat", FriendKind.OTTER, ItemSlot.HAT, "Diving helmet", 30),
        ShopItem("turtle_hat", FriendKind.TURTLE, ItemSlot.HAT, "Shell sun hat", 35),
        ShopItem("axolotl_hat", FriendKind.AXOLOTL, ItemSlot.HAT, "Party crown", 30),
    )

    fun byId(id: String): ShopItem? = items.firstOrNull { it.id == id }

    fun forKind(kind: FriendKind): List<ShopItem> = items.filter { it.kind == kind }

    /** Kinds that have something to sell, in the enum's order. */
    fun groups(): List<Pair<FriendKind, List<ShopItem>>> =
        FriendKind.entries.mapNotNull { kind ->
            forKind(kind).takeIf { it.isNotEmpty() }?.let { kind to it }
        }
}
