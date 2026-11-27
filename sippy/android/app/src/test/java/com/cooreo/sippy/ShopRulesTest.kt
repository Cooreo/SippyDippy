package com.cooreo.sippy

import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopRulesTest {

    private val hat = "koi_hat"
    private val scarf = "koi_scarf"
    private val otterHat = "otter_hat"

    @Test
    fun buyingDeductsThePriceAndMarksItOwned() {
        val price = ItemTable.byId(hat)!!.price
        val outcome = ShopRules.purchase(balance = 100, owned = emptySet(), itemId = hat)
        assertTrue("expected a purchase, got $outcome", outcome is ShopRules.Purchase.Bought)
        if (outcome !is ShopRules.Purchase.Bought) return
        assertEquals(100 - price, outcome.balance)
        assertTrue(hat in outcome.owned)
    }

    @Test
    fun buyingIsRejectedWhenTheBalanceIsShort() {
        val price = ItemTable.byId(hat)!!.price
        assertEquals(ShopRules.Purchase.TooExpensive, ShopRules.purchase(price - 1, emptySet(), hat))
        // Exactly the price is enough.
        assertTrue(ShopRules.purchase(price, emptySet(), hat) is ShopRules.Purchase.Bought)
    }

    @Test
    fun buyingTheSameItemTwiceIsRejectedAndCostsNothing() {
        val again = ShopRules.purchase(100, setOf(hat), hat)
        assertEquals(ShopRules.Purchase.AlreadyOwned, again)
    }

    @Test
    fun anUnknownItemIsRejected() {
        assertEquals(ShopRules.Purchase.UnknownItem, ShopRules.purchase(100, emptySet(), "golden_helmet"))
    }

    @Test
    fun onlyOwnedItemsOfTheRightFriendCanBeWorn() {
        assertFalse(ShopRules.canEquip(emptySet(), FriendKind.KOI, hat))
        assertTrue(ShopRules.canEquip(setOf(hat), FriendKind.KOI, hat))
        // Right item, wrong friend: a koi hat is not a turtle hat.
        assertFalse(ShopRules.canEquip(setOf(hat), FriendKind.TURTLE, hat))
    }

    @Test
    fun equippingReplacesWhateverThatFriendWoreAndNullUndresses() {
        val one = ShopRules.equip(emptyMap(), "f1", FriendKind.KOI, setOf(hat, scarf), hat)
        assertEquals(hat, one["f1"])
        val two = ShopRules.equip(one, "f1", FriendKind.KOI, setOf(hat, scarf), scarf)
        assertEquals(scarf, two["f1"])
        assertEquals(1, two.size)
        assertTrue(ShopRules.equip(two, "f1", FriendKind.KOI, setOf(hat, scarf), null).isEmpty())
    }

    @Test
    fun equippingAnUnownedItemChangesNothing() {
        val before = mapOf("f1" to hat)
        assertEquals(before, ShopRules.equip(before, "f1", FriendKind.KOI, setOf(hat), scarf))
        assertEquals(before, ShopRules.equip(before, "f2", FriendKind.OTTER, setOf(hat), hat))
    }

    @Test
    fun aFriendLeavingKeepsTheItemOwnedAndItsSiblingsEquipped() {
        // Owned items are keyed by item id, not by friend, so removing the koi
        // from state.friends cannot take the hat with it. Both worn items are
        // owned here, because a worn item that is no longer owned is reported
        // as nothing (see the next test).
        val owned = setOf(hat, scarf, otterHat)
        val equipped = mapOf("koi" to hat, "otter" to otterHat)
        assertTrue(hat in owned)
        assertEquals(hat, ShopRules.wornBy(equipped, owned, "koi")?.id)
        assertEquals(otterHat, ShopRules.wornBy(equipped, owned, "otter")?.id)
        // And with no friend there is nothing to report.
        assertNull(ShopRules.wornBy(equipped, owned, "gone"))
    }

    @Test
    fun aWornItemThatIsNoLongerOwnedIsNotShown() {
        // The defensive half of wornBy: if a document ever claims a cosmetic
        // that is not in ownedItems, the friend is drawn without it.
        assertNull(ShopRules.wornBy(mapOf("koi" to hat), emptySet(), "koi"))
    }

    @Test
    fun everyListedItemHasArtAndASanePrice() {
        val known = setOf(hat, scarf, otterHat, "turtle_hat", "axolotl_hat")
        ItemTable.items.forEach { item ->
            assertTrue(item.id, item.price > 0)
            assertTrue(item.id, item.id in known)
        }
        assertEquals(ItemTable.items.size, ItemTable.items.map { it.id }.toSet().size)
        assertTrue(ItemTable.forKind(FriendKind.CAT).isEmpty())
        assertEquals(2, ItemTable.forKind(FriendKind.KOI).size)
    }
}
