package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.StreakState
import com.cooreo.sippy.ui.screens.greeting
import com.cooreo.sippy.ui.screens.moodFor
import com.cooreo.sippy.ui.screens.moodLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRulesTest {

    private val dippy = Friend(id = "dippy", kind = FriendKind.DIPPY, name = "Dippy", grownAtStreak = 7)
    private val fresh = Friend(id = "sprout", kind = FriendKind.PLANT, name = "Sprout")

    private fun state(streakDays: Int) = StoredState(
        streak = StreakState(currentDays = streakDays, bestDays = streakDays),
    )

    @Test
    fun greetingUsesTheNameOnlyWhenOneIsSet() {
        assertEquals("Good morning", greeting("", hour = 8))
        assertEquals("Good morning", greeting("   ", hour = 8))
        assertEquals("Good morning, Maja", greeting(" Maja ", hour = 8))
        assertEquals("Good afternoon, Maja", greeting("Maja", hour = 14))
        assertEquals("Good evening, Maja", greeting("Maja", hour = 21))
        assertEquals("Still up, Maja", greeting("Maja", hour = 3))
    }

    @Test
    fun nothingLoggedTodayLeavesFriendsSleepy() {
        assertEquals(
            Mood.SLEEPY,
            moodFor(fresh, state(0), goalMetToday = false, hasEntriesToday = false),
        )
    }

    @Test
    fun goalMetWithAStreakMakesFriendsDance() {
        assertEquals(
            Mood.DANCING,
            moodFor(dippy, state(4), goalMetToday = true, hasEntriesToday = true),
        )
        assertEquals(
            Mood.HAPPY,
            moodFor(dippy, state(1), goalMetToday = true, hasEntriesToday = true),
        )
    }

    @Test
    fun aMissedStreakAfterGrowthIsSad() {
        // The friend grew at 7 days; the current streak fell below that.
        assertEquals(
            Mood.SAD,
            moodFor(dippy, state(0), goalMetToday = false, hasEntriesToday = false),
        )
    }

    @Test
    fun everyMoodHasALine() {
        Mood.entries.forEach { mood ->
            val line = moodLine("Dippy", mood)
            assertTrue("blank line for $mood", line.isNotBlank())
            assertTrue("line should name the friend: $line", line.startsWith("Dippy"))
        }
    }
}
