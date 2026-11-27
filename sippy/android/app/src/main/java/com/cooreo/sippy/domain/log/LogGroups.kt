package com.cooreo.sippy.domain.log

import com.cooreo.sippy.model.LogEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One day of the log, ready for the History screen: newest entry first inside the
 * day, with the day's totals and whether the goal was met already worked out.
 */
data class DayGroup(
    val dayEpoch: Long,
    val label: String,
    val entries: List<LogEntry>,
    val volumeMl: Int,
    val countedMl: Int,
    val goalMet: Boolean,
)

/**
 * Day grouping for the dated log (round 8, A3). Pure: the caller passes "today",
 * so labels and goal ticks are deterministic and unit tested — the UI never
 * formats a date itself.
 */
object LogGroups {

    private val sameYearFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val otherYearFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

    /**
     * Newest first. Identical timestamps fall back to the entry id (descending),
     * so a re-sort of the same data can never reorder two entries — that is what
     * makes the list stable and the tests meaningful.
     */
    val newestFirst: Comparator<LogEntry> =
        compareByDescending<LogEntry> { it.atEpochMs }.thenByDescending { it.id }

    /** "Today", "Yesterday", then "Mon 24 Nov" — with the year when it differs. */
    fun labelFor(dayEpoch: Long, todayEpoch: Long): String = when (dayEpoch) {
        todayEpoch -> "Today"
        todayEpoch - 1 -> "Yesterday"
        else -> {
            val date = LocalDate.ofEpochDay(dayEpoch)
            val sameYear = date.year == LocalDate.ofEpochDay(todayEpoch).year
            date.format(if (sameYear) sameYearFormat else otherYearFormat)
        }
    }

    /** Days newest first; inside each day, entries newest first. */
    fun byDay(entries: List<LogEntry>, todayEpoch: Long, goalMl: Int = 0): List<DayGroup> =
        entries
            .groupBy { it.dayEpoch }
            .entries
            .sortedByDescending { it.key }
            .map { (day, list) ->
                val sorted = list.sortedWith(newestFirst)
                val counted = sorted.sumOf { it.hydratedMl }
                DayGroup(
                    dayEpoch = day,
                    label = labelFor(day, todayEpoch),
                    entries = sorted,
                    volumeMl = sorted.sumOf { it.volumeMl },
                    countedMl = counted,
                    goalMet = goalMl > 0 && counted >= goalMl,
                )
            }

    /** The newest entries of one day — what Home shows above "See all". */
    fun newestOfDay(entries: List<LogEntry>, todayEpoch: Long, limit: Int): List<LogEntry> =
        entries
            .filter { it.dayEpoch == todayEpoch }
            .sortedWith(newestFirst)
            .take(limit)
}
