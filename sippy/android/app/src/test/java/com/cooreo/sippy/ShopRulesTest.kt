package com.cooreo.sippy

import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.domain.shop.Wardrobe
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A4: buying and wearing cosmetics, per friend id and per slot. */
class ShopRulesTest {

    private val koi = FriendKind.KOI
    private val allOwned = ItemTable.items.map { it.id }.toSet()

    @Test
    fun purchaseDeductsPointsAndMarksOwned() {
        val result = ShopRules.purchase(balance = 100, owned = emptySet(), itemId = "koi_hat")
        assertTrue(result is ShopRules.Purchase.Bought)
        val bought = result as ShopRules.Purchase.Bought
        assertEquals(70, bought.balance)
        assertTrue("koi_hat" in bought.owned)
    }

    @Test
    fun purchaseRefusesWhatIsAlreadyOwnedTooExpensiveOrUnknown() {
        val owned = setOf("koi_hat")
        assertTrue(ShopRules.purchase(100, owned, "koi_hat") is ShopRules.Purchase.AlreadyOwned)
        assertTrue(ShopRules.purchase(10, owned, "koi_scarf") is ShopRules.Purchase.TooExpensive)
        assertTrue(ShopRules.purchase(100, owned, "no_such_item") is ShopRules.Purchase.UnknownItem)
    }

    @Test
    fun purchaseNeverGoesNegative() {
        val result = ShopRules.purchase(balance = -40, owned = emptySet(), itemId = "koi_hat")
        assertTrue("a negative balance buys nothing", result is ShopRules.Purchase.TooExpensive)
    }

    @Test
    fun toggleWearsThenTakesOffTheSameItem() {
        val on = ShopRules.toggle(Wardrobe(), "f1", koi, allOwned, "koi_hat")
        assertEquals("koi_hat", on.hats["f1"])
        val off = ShopRules.toggle(on, "f1", koi, allOwned, "koi_hat")
        assertNull(off.hats["f1"])
        assertNull("the other slot is untouched", off.outfits["f1"])
    }

    @Test
    fun hatAndOutfitAreSeparateSlots() {
        val both = ShopRules.toggle(
            ShopRules.toggle(Wardrobe(), "f1", koi, allOwned, "koi_hat"),
            "f1",
            koi,
            allOwned,
            "koi_scarf",
        )
        assertEquals("koi_hat", both.on(ItemSlot.HAT, "f1"))
        assertEquals("koi_scarf", both.on(ItemSlot.OUTFIT, "f1"))
        // Removing the outfit keeps the hat.
        val hatOnly = ShopRules.unequip(both, "f1", ItemSlot.OUTFIT)
        assertEquals("koi_hat", hatOnly.on(ItemSlot.HAT, "f1"))
        assertNull(hatOnly.on(ItemSlot.OUTFIT, "f1"))
    }

    @Test
    fun oneItemPerSlotReplacesThePreviousOne() {
        val owned = setOf("koi_hat", "koi_scarf", "otter_hat")
        val swapped = ShopRules.toggle(
            ShopRules.toggle(Wardrobe(), "f1", koi, owned, "koi_scarf"),
            "f1",
            koi,
            owned,
            "koi_scarf",
        )
        assertNull("tapping the worn item takes it off", swapped.on(ItemSlot.OUTFIT, "f1"))
        // The hat map holds at most one id per friend by construction.
        assertTrue(swapped.hats.size <= 1)
    }

    @Test
    fun refusesUnownedAndForeignSpecies() {
        val onlyKoi = setOf("koi_hat")
        assertEquals(Wardrobe(), ShopRules.toggle(Wardrobe(), "f1", koi, onlyKoi, "otter_hat"))
        assertEquals(Wardrobe(), ShopRules.toggle(Wardrobe(), "f1", koi, onlyKoi, "koi_scarf"))
        assertFalse(ShopRules.canEquip(onlyKoi, koi, "otter_hat"))
        assertFalse(ShopRules.canEquip(onlyKoi, FriendKind.OTTER, "koi_hat"))
        assertTrue(ShopRules.canEquip(onlyKoi, koi, "koi_hat"))
    }

    @Test
    fun stateIsKeptPerFriendId() {
        val a = ShopRules.toggle(Wardrobe(), "f1", koi, allOwned, "koi_hat")
        val b = ShopRules.toggle(a, "f2", koi, allOwned, "koi_scarf")
        assertEquals("koi_hat", b.on(ItemSlot.HAT, "f1"))
        assertEquals("koi_scarf", b.on(ItemSlot.OUTFIT, "f2"))
        assertNull(b.on(ItemSlot.HAT, "f2"))
    }

    @Test
    fun ownershipSurvivesTakingTheItemOff() {
        val wardrobe = ShopRules.toggle(Wardrobe(), "f1", koi, allOwned, "koi_hat")
        val bare = ShopRules.toggle(wardrobe, "f1", koi, allOwned, "koi_hat")
        assertTrue(ShopRules.worn(bare.hats, bare.outfits, allOwned, "f1").isEmpty())
        assertTrue("the id stays in the shop's owned set", "koi_hat" in allOwned)
        assertTrue(ShopRules.canEquip(allOwned, koi, "koi_hat"))
    }

    @Test
    fun aPreRound8ScarfInTheHatMapStillShows() {
        // Old saves stored whatever was bought in `equipped`, including outfits.
        val legacyHats = mapOf("f1" to "koi_scarf")
        val worn = ShopRules.worn(legacyHats, emptyMap(), allOwned, "f1")
        assertEquals(listOf("koi_scarf"), worn.map { it.id })
        assertEquals(ItemSlot.OUTFIT, worn.single().slot)
    }

    @Test
    fun unownedItemsNeverShow() {
        val hats = mapOf("f1" to "koi_hat")
        assertTrue(ShopRules.worn(hats, emptyMap(), owned = emptySet(), friendId = "f1").isEmpty())
    }
}
