package com.cooreo.sippy

import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterMathTest {

    private fun entry(day: Long, hydrated: Int) = LogEntry(
        id = "id-$day-$hydrated",
        dayEpoch = day,
        atEpochMs = day * 86_400_000L,
        drinkId = "water",
        volumeMl = hydrated,
        hydratedMl = hydrated,
        sugary = false,
    )

    @Test
    fun volumeIsScaledByHydrationFactor() {
        assertEquals(250, WaterMath.hydrate(250, 1.0f))
        assertEquals(180, WaterMath.hydrate(200, 0.9f))
        assertEquals(165, WaterMath.hydrate(330, 0.5f))
        assertEquals(0, WaterMath.hydrate(0, 1.0f))
        assertEquals(0, WaterMath.hydrate(-100, 1.0f))
    }

    @Test
    fun hydrationFactorIsClampedToZeroOne() {
        assertEquals(100, WaterMath.hydrate(100, 1.7f))
        assertEquals(0, WaterMath.hydrate(100, -1f))
    }

    @Test
    fun drinkTableAndWaterMathAgree() {
        DrinkTable.drinks.forEach { drink ->
            assertEquals(
                "drink ${drink.id}",
                WaterMath.hydrate(drink.defaultVolumeMl, drink.hydrationFactor),
                drink.hydratedMl(drink.defaultVolumeMl),
            )
        }
    }

    @Test
    fun goalFractionStaysInsideZeroOne() {
        assertEquals(0f, WaterMath.goalFraction(0, 2000), 0.0001f)
        assertEquals(0.5f, WaterMath.goalFraction(1000, 2000), 0.0001f)
        assertEquals(1f, WaterMath.goalFraction(2000, 2000), 0.0001f)
        assertEquals(1f, WaterMath.goalFraction(9000, 2000), 0.0001f)
        assertEquals(0f, WaterMath.goalFraction(500, 0), 0.0001f)
    }

    @Test
    fun goalIsMetAtOrAboveGoal() {
        assertFalse(WaterMath.goalMet(1999, 2000))
        assertTrue(WaterMath.goalMet(2000, 2000))
        assertTrue(WaterMath.goalMet(2500, 2000))
        assertFalse(WaterMath.goalMet(0, 0))
    }

    @Test
    fun remainingNeverGoesNegative() {
        assertEquals(1000, WaterMath.remainingMl(1000, 2000))
        assertEquals(0, WaterMath.remainingMl(2500, 2000))
    }

    @Test
    fun goalClampUsesBriefBounds() {
        assertEquals(750, WaterMath.clampGoal(10))
        assertEquals(750, WaterMath.clampGoal(750))
        assertEquals(2000, WaterMath.clampGoal(2000))
        assertEquals(3000, WaterMath.clampGoal(9000))
    }

    @Test
    fun todaysTotalSumsEntries() {
        val entries = listOf(entry(1, 250), entry(1, 330), entry(2, 500))
        assertEquals(1080, WaterMath.hydratedMl(entries))
        assertEquals(0, WaterMath.hydratedMl(emptyList()))
    }
}
