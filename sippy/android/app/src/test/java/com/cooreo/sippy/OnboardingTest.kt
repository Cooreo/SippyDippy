package com.cooreo.sippy

import com.cooreo.sippy.domain.onboarding.Onboarding
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Onboarding halt/resume and the goal page's clamping. Pure JVM. */
class OnboardingTest {

    @Test
    fun aKillMidFlowReopensOnTheSamePage() {
        Onboarding.PAGE_COUNT.let { count ->
            for (page in 0 until count) {
                assertEquals(page, Onboarding.startPage(page, complete = false))
            }
        }
    }

    @Test
    fun storedPagesOutsideTheRangeAreClamped() {
        assertEquals(Onboarding.PAGE_WELCOME, Onboarding.startPage(-7, complete = false))
        assertEquals(Onboarding.PAGE_FRIEND, Onboarding.startPage(99, complete = false))
        assertEquals(Onboarding.PAGE_WELCOME, Onboarding.clampPage(-1))
        assertEquals(Onboarding.PAGE_FRIEND, Onboarding.clampPage(3))
    }

    @Test
    fun finishedOnboardingHasNoPage() {
        assertNull(Onboarding.startPage(Onboarding.PAGE_GOAL, complete = true))
    }

    @Test
    fun pagingNeverLandsOutsideTheRange() {
        assertFalse(Onboarding.isLastPage(Onboarding.PAGE_WELCOME))
        assertTrue(Onboarding.isLastPage(Onboarding.PAGE_FRIEND))
        assertEquals(Onboarding.PAGE_GOAL, Onboarding.nextPage(Onboarding.PAGE_WELCOME))
        assertEquals(Onboarding.PAGE_FRIEND, Onboarding.nextPage(Onboarding.PAGE_GOAL))
        // "Next" on the last page must not overflow past the pager.
        assertEquals(Onboarding.PAGE_FRIEND, Onboarding.nextPage(Onboarding.PAGE_FRIEND))
    }

    @Test
    fun theGoalSnapsToFiftyMillilitreSteps() {
        assertEquals(2000, Onboarding.snapGoal(2000f))
        assertEquals(2000, Onboarding.snapGoal(2024f))
        assertEquals(2050, Onboarding.snapGoal(2026f))
        assertEquals(1500, Onboarding.snapGoal(1499f))
    }

    @Test
    fun theGoalClampsTo750And3000() {
        assertEquals(Settings.MIN_GOAL_ML, Onboarding.snapGoal(0f))
        assertEquals(Settings.MIN_GOAL_ML, Onboarding.snapGoal(700f))
        assertEquals(Settings.MAX_GOAL_ML, Onboarding.snapGoal(3100f))
        assertEquals(750, WaterMath.clampGoal(1))
        assertEquals(3000, WaterMath.clampGoal(99_999))
        assertEquals(2000, WaterMath.clampGoal(Settings.DEFAULT_GOAL_ML))
    }

    @Test
    fun anOlderStoredGoalIsBroughtIntoTheAllowedRange() {
        assertEquals(750, Onboarding.goalFor(120))
        assertEquals(3000, Onboarding.goalFor(5000))
        assertEquals(2000, Onboarding.goalFor(1990))
        assertEquals(2000, Onboarding.goalFor(Settings.DEFAULT_GOAL_ML))
    }

    @Test
    fun sliderGranularityCoversTheWholeRange() {
        assertEquals(
            ((Settings.MAX_GOAL_ML - Settings.MIN_GOAL_ML) / Onboarding.GOAL_STEP_ML) - 1,
            Onboarding.GOAL_SLIDER_STEPS,
        )
        Onboarding.GOAL_PRESETS.forEach { preset ->
            assertTrue(preset in Settings.MIN_GOAL_ML..Settings.MAX_GOAL_ML)
            assertEquals(preset, Onboarding.snapGoal(preset.toFloat()))
        }
    }

    @Test
    fun progressIsAFractionOfTheAllowedRange() {
        assertEquals(0f, Onboarding.goalProgress(Settings.MIN_GOAL_ML), 0.0001f)
        assertEquals(1f, Onboarding.goalProgress(Settings.MAX_GOAL_ML), 0.0001f)
        assertTrue(Onboarding.goalProgress(2000) in 0.5f..0.6f)
    }
}
