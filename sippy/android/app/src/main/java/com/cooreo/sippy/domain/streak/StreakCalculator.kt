package com.cooreo.sippy.domain.streak

import com.cooreo.sippy.model.LogEntry

/**
 * Streaks are day-based (local epoch-day), goal-based and gap-tolerant in one
 * specific way: a day that never reached the goal breaks the run; "today" is
 * still open until midnight, so the streak shows yesterday-to-today until the
 * goal is hit today.
 */
object StreakCalculator {

    val MILESTONES = listOf(3, 7, 14, 30, 100)

    /** Days whose credited total reached the goal. */
    fun metGoalDays(entries: List<LogEntry>, goalMl: Int): Set<Long> {
        if (goalMl <= 0) return emptySet()
        return entries
            .groupBy { it.dayEpoch }
            .filterValues { day -> day.sumOf { it.hydratedMl } >= goalMl }
            .keys
    }

    /**
     * Run of consecutive goal days ending today, or ending yesterday when
     * today has not reached the goal yet (the run is not broken until the day
     * is over).
     */
    fun currentStreak(entries: List<LogEntry>, goalMl: Int, todayEpoch: Long): Int {
        val met = metGoalDays(entries, goalMl)
        if (met.isEmpty()) return 0
        var day = if (met.contains(todayEpoch)) todayEpoch else todayEpoch - 1
        var run = 0
        while (met.contains(day)) {
            run += 1
            day -= 1
        }
        return run
    }

    fun bestStreak(entries: List<LogEntry>, goalMl: Int): Int {
        val met = metGoalDays(entries, goalMl)
        if (met.isEmpty()) return 0
        var best = 0
        for (day in met) {
            // only start counting at the beginning of a run
            if (met.contains(day - 1)) continue
            var d = day
            var run = 0
            while (met.contains(d)) {
                run += 1
                d += 1
            }
            if (run > best) best = run
        }
        return best
    }

    /** The milestone reached exactly now, or null. */
    fun milestoneReached(streakDays: Int): Int? =
        MILESTONES.firstOrNull { it == streakDays }

    /** Friend growth: the highest milestone the streak has passed. */
    fun growthFor(streakDays: Int): Int = MILESTONES.lastOrNull { streakDays >= it } ?: 0
}
