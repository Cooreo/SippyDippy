package com.cooreo.sippy

import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.ui.components.DrinkGroup
import com.cooreo.sippy.ui.components.DrinkGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The icon tint map is a parallel table, so it is tested against the one drink
 * table: every drink gets an explicit group, and water stays water.
 */
class DrinkGroupsTest {

    @Test
    fun everyDrinkHasAnExplicitIconGroup() {
        val missing = DrinkTable.drinks.map { it.id }.filterNot { it in DrinkGroups.knownIds }
        assertTrue("drinks without an icon group: $missing", missing.isEmpty())
    }

    @Test
    fun noGroupIsForAnIdThatNoDrinkUses() {
        val ids = DrinkTable.drinks.map { it.id }.toSet()
        val orphans = DrinkGroups.knownIds.filterNot { it in ids }
        assertTrue("icon groups for unknown drinks: $orphans", orphans.isEmpty())
    }

    @Test
    fun waterIsWater() {
        assertEquals(DrinkGroup.WATER, DrinkGroups.of("water"))
        assertEquals(DrinkGroup.COFFEE, DrinkGroups.of("espresso"))
        assertEquals(DrinkGroup.ALCOHOL, DrinkGroups.of("beer"))
    }

    @Test
    fun unknownIdsFallBackToWater() {
        assertEquals(DrinkGroup.WATER, DrinkGroups.of("not-a-drink"))
    }
}
