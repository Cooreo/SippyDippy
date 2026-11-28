package com.cooreo.sippy.domain.water

/**
 * When "Goal reached" is allowed to happen (round 9, 2.2).
 *
 * It is a crossing, once a day. The crossing half already existed; the once-a-day
 * half did not, so undoing a log and logging it again replayed the whole thing. The
 * day the last celebration happened is persisted, because "once a day" that resets on
 * a process death is not once a day.
 *
 * Pure, so the state machine is testable without a controller.
 */
object CelebrationRules {

    /** How long the celebration runs, and how long it takes to fade out. */
    const val DURATION_MS = 3500
    const val FADE_MS = 400

    /**
     * True when this log is the one that takes the day over its goal, and no
     * celebration has been shown for [today] yet.
     */
    fun shouldCelebrate(
        goalMetBefore: Boolean,
        goalMetAfter: Boolean,
        today: Long,
        lastCelebratedDay: Long?,
    ): Boolean = !goalMetBefore && goalMetAfter && lastCelebratedDay != today

    /**
     * The percentage to draw next to the goal, as a whole number, capped at 100 for
     * the ring and reported separately when it goes over.
     */
    fun percent(hydratedMl: Int, goalMl: Int): Int {
        if (goalMl <= 0) return 0
        return (hydratedMl * 100) / goalMl
    }

    /** How far over the goal the day is, or 0. */
    fun overMl(hydratedMl: Int, goalMl: Int): Int = (hydratedMl - goalMl).coerceAtLeast(0)

    /** "of 2000 ml" — the unit the number under the orb is measured in. */
    fun goalLabel(goalMl: Int): String = "of $goalMl ml"
}
