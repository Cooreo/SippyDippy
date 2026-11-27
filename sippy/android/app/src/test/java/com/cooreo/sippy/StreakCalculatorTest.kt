package com.cooreo.sippy

import com.cooreo.sippy.domain.streak.StreakCalculator
import com.cooreo.sippy.model.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakCalculatorTest {

    private fun sip(day: Long, hydrated: Int) = LogEntry(
        id = "d$day-$hydrated",
        dayEpoch = day,
        atEpochMs = day * 86_400_000L,
        drinkId = "water",
        volumeMl = hydrated,
        hydratedMl = hydrated,
        sugary = false,
    )

    @Test
    fun threeConsecutiveGoalDaysIsAThreeDayStreak() {
        val log = listOf(sip(10, 2000), sip(11, 2000), sip(12, 2000))
        assertEquals(3, StreakCalculator.currentStreak(log, 2000, todayEpoch = 12))
        assertEquals(3, StreakCalculator.bestStreak(log, 2000))
    }

    @Test
    fun todayStillCountsYesterdayRunUntilTheDayEnds() {
        val log = listOf(sip(10, 2000), sip(11, 2000))
        // Today (12) has nothing yet: the run is not broken yet.
        assertEquals(2, StreakCalculator.currentStreak(log, 2000, todayEpoch = 12))
        // A day later (13) yesterday missed -> streak is 0.
        assertEquals(0, StreakCalculator.currentStreak(log, 2000, todayEpoch = 13))
    }

    @Test
    fun aMissedDayBreaksTheRun() {
        val log = listOf(sip(8, 2000), sip(9, 2000), sip(11, 2000), sip(12, 2000))
        assertEquals(2, StreakCalculator.currentStreak(log, 2000, todayEpoch = 12))
        assertEquals(2, StreakCalculator.bestStreak(log, 2000))
    }

    @Test
    fun severalSipsInADayAddUpToTheGoal() {
        val log = listOf(sip(12, 1000), sip(12, 1000))
        assertEquals(1, StreakCalculator.currentStreak(log, 2000, todayEpoch = 12))
        assertEquals(0, StreakCalculator.currentStreak(log, 2001, todayEpoch = 12))
    }

    @Test
    fun bestStreakSurvivesALaterGap() {
        val log = listOf(sip(1, 2000), sip(2, 2000), sip(3, 2000), sip(9, 2000))
        assertEquals(1, StreakCalculator.currentStreak(log, 2000, todayEpoch = 9))
        assertEquals(3, StreakCalculator.bestStreak(log, 2000))
    }

    @Test
    fun emptyLogHasNoStreak() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList(), 2000, 5))
        assertEquals(0, StreakCalculator.bestStreak(emptyList(), 2000))
    }

    @Test
    fun milestonesAreExactAndGrowthIsTheHighestPassed() {
        assertEquals(3, StreakCalculator.milestoneReached(3))
        assertEquals(100, StreakCalculator.milestoneReached(100))
        assertNull(StreakCalculator.milestoneReached(4))
        assertEquals(0, StreakCalculator.growthFor(2))
        assertEquals(3, StreakCalculator.growthFor(6))
        assertEquals(30, StreakCalculator.growthFor(30))
        assertEquals(100, StreakCalculator.growthFor(400))
    }
}
