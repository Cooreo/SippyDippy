package com.cooreo.sippy.domain.log

import com.cooreo.sippy.model.LogEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One day of the log, ready for the History screen: newest entry first inside
 * the day, with the day's totals already summed.
 */
data class DayGroup(
    val dayEpoch: Long,
    val label: String,
    val entries: List<LogEntry>,
    val volumeMl: Int,
    val countedMl: Int,
)

/**
 * Day grouping for the dated log (round 7, B1). Pure: the caller passes
 * "today", so the labels are deterministic and unit tested — the UI never
 * formats a date itself.
 */
object LogGroups {

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    /** "Today", "Yesterday", then the date. */
    fun labelFor(dayEpoch: Long, todayEpoch: Long): String = when (dayEpoch) {
        todayEpoch -> "Today"
        todayEpoch - 1 -> "Yesterday"
        else -> LocalDate.ofEpochDay(dayEpoch).format(dateFormat)
    }

    /** Days newest first; inside each day, entries newest first. */
    fun byDay(entries: List<LogEntry>, todayEpoch: Long): List<DayGroup> =
        entries
            .groupBy { it.dayEpoch }
            .entries
            .sortedByDescending { it.key }
            .map { (day, list) ->
                val sorted = list.sortedByDescending { it.atEpochMs }
                DayGroup(
                    dayEpoch = day,
                    label = labelFor(day, todayEpoch),
                    entries = sorted,
                    volumeMl = sorted.sumOf { it.volumeMl },
                    countedMl = sorted.sumOf { it.hydratedMl },
                )
            }

    /** The newest entries of one day — what Home shows above "See all". */
    fun newestOfDay(entries: List<LogEntry>, todayEpoch: Long, limit: Int): List<LogEntry> =
        entries
            .filter { it.dayEpoch == todayEpoch }
            .sortedByDescending { it.atEpochMs }
            .take(limit)
}
