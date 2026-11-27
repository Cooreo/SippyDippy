package com.cooreo.sippy.domain.friends

/** The four moods the brief allows. Art is picked per (kind, mood). */
enum class Mood { HAPPY, DANCING, SLEEPY, SAD }

/**
 * Mood is derived, never stored: it is a pure function of today's progress.
 *  - DANCING: goal met and a streak of 3+;
 *  - HAPPY:   goal met;
 *  - SLEEPY:  nothing logged yet today;
 *  - SAD:     there was a streak of 3+ and yesterday missed the goal.
 */
object MoodRules {
    fun forDay(
        goalMetToday: Boolean,
        hasEntriesToday: Boolean,
        currentStreakDays: Int,
        yesterdayMissedAfterStreak: Boolean,
    ): Mood = when {
        goalMetToday && currentStreakDays >= 3 -> Mood.DANCING
        goalMetToday -> Mood.HAPPY
        !hasEntriesToday && yesterdayMissedAfterStreak -> Mood.SAD
        !hasEntriesToday -> Mood.SLEEPY
        yesterdayMissedAfterStreak -> Mood.SAD
        else -> Mood.HAPPY
    }
}
