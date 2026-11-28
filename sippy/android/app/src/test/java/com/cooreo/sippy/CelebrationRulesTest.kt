package com.cooreo.sippy

import com.cooreo.sippy.domain.water.CelebrationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 9, 2.2: the crossing, once a day. */
class CelebrationRulesTest {

    @Test
    fun theCrossingCelebrates() {
        assertTrue(CelebrationRules.shouldCelebrate(false, true, today = 100, lastCelebratedDay = null))
    }

    @Test
    fun drinkingPastTheGoalAgainDoesNot() {
        assertFalse(CelebrationRules.shouldCelebrate(true, true, 100, null))
    }

    @Test
    fun undoingAndCrossingAgainInTheSameDayDoesNot() {
        assertFalse(CelebrationRules.shouldCelebrate(false, true, 100, lastCelebratedDay = 100))
    }

    @Test
    fun theNextDayCelebratesAgain() {
        assertTrue(CelebrationRules.shouldCelebrate(false, true, 101, lastCelebratedDay = 100))
    }

    @Test
    fun aFullStateMachineOverADay() {
        var last: Long? = null
        val today = 500L
        var met = false
        val events = mutableListOf<String>()
        // log up to the goal, undo, log again, go over, undo and re-cross.
        for (step in listOf(1500, 600, -600, 600, 800, -800, 800)) {
            val before = met
            val total = if (step > 0) step else 0
            met = (total >= 2000)
            if (CelebrationRules.shouldCelebrate(before, met, today, last)) {
                events += "celebrated"
                last = today
            }
        }
        assertEquals(1, events.size)
        assertEquals(today, last)
    }

    @Test
    fun percentAndOverflow() {
        assertEquals(0, CelebrationRules.percent(0, 2000))
        assertEquals(50, CelebrationRules.percent(1000, 2000))
        assertEquals(100, CelebrationRules.percent(2000, 2000))
        assertEquals(120, CelebrationRules.percent(2400, 2000))
        assertEquals(0, CelebrationRules.percent(500, 0))
        assertEquals(400, CelebrationRules.overMl(2400, 2000))
        assertEquals(0, CelebrationRules.overMl(1900, 2000))
        assertEquals("of 2000 ml", CelebrationRules.goalLabel(2000))
    }

    @Test
    fun theCelebrationIsLongEnoughToRead() {
        assertTrue(CelebrationRules.DURATION_MS >= 3000)
        assertTrue(CelebrationRules.FADE_MS in 300..500)
    }
}
