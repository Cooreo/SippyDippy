package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodRules
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodRulesTest {

    @Test
    fun goalMetWithStreakDances() {
        assertEquals(Mood.DANCING, MoodRules.forDay(true, true, 3, false))
        assertEquals(Mood.HAPPY, MoodRules.forDay(true, true, 2, false))
    }

    @Test
    fun nothingLoggedIsSleepyUnlessTheStreakSlipped() {
        assertEquals(Mood.SLEEPY, MoodRules.forDay(false, false, 1, false))
        assertEquals(Mood.SAD, MoodRules.forDay(false, false, 0, true))
    }

    @Test
    fun aSlippedStreakIsSadEvenAfterDrinking() {
        assertEquals(Mood.SAD, MoodRules.forDay(false, true, 0, true))
        // Still working on today's goal, no slip: cheerful.
        assertEquals(Mood.HAPPY, MoodRules.forDay(false, true, 0, false))
    }
}
