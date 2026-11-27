package com.cooreo.sippy

import com.cooreo.sippy.domain.drinks.DrinkCategory
import com.cooreo.sippy.domain.drinks.DrinkTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B3/B4: every drink in the table lands in a family, and every family has an icon. */
class DrinkCategoryTest {

    @Test
    fun everyDrinkMapsToACategory() {
        val categories = DrinkTable.drinks.map { DrinkCategory.of(it.id) }
        assertEquals(DrinkTable.drinks.size, categories.size)
    }

    @Test
    fun onlySoupAndUnknownIdsFallIntoOther() {
        // Soup is in the drink table because it hydrates, not because it is a
        // drink family; it is the one legitimate OTHER. Anything else landing
        // there means the mapping fell behind the table.
        val other = DrinkTable.drinks
            .filter { DrinkCategory.of(it.id) == DrinkCategory.OTHER }
            .map { it.id }
        assertEquals(listOf("soup"), other)
        assertEquals(DrinkCategory.OTHER, DrinkCategory.of("not-a-drink"))
    }

    @Test
    fun theFourTintFamiliesAreNotEmpty() {
        val byCategory = DrinkTable.drinks.groupBy { DrinkCategory.of(it.id) }
        listOf(
            DrinkCategory.WATER,
            DrinkCategory.TEA,
            DrinkCategory.COFFEE,
            DrinkCategory.JUICE,
        ).forEach { category ->
            assertNotNull("$category has no drinks", byCategory[category])
        }
    }

    @Test
    fun familiesSplitTheObviousCases() {
        assertEquals(DrinkCategory.WATER, DrinkCategory.of("water"))
        assertEquals(DrinkCategory.SPARKLING, DrinkCategory.of("sparkling"))
        assertEquals(DrinkCategory.TEA, DrinkCategory.of("green-tea"))
        assertEquals(DrinkCategory.COFFEE, DrinkCategory.of("espresso"))
        assertEquals(DrinkCategory.JUICE, DrinkCategory.of("apple-juice"))
        assertEquals(DrinkCategory.MILK, DrinkCategory.of("oat-milk"))
        assertEquals(DrinkCategory.ALCOHOL, DrinkCategory.of("wine"))
        assertNotEquals(DrinkCategory.of("water"), DrinkCategory.of("coffee"))
    }

    @Test
    fun everyCategoryHasAnIconName() {
        DrinkCategory.entries.forEach { category ->
            val name = DrinkCategory.iconName(category)
            assertNotNull(name)
            assertTrue("icon name for $category must be a drawable name", name!!.startsWith("drink_"))
        }
    }
}
