package com.cooreo.sippy.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Local-day helpers. Everything the app stores as a "day" is an epoch-day. */
object Dates {

    fun todayEpochDay(now: LocalDate = LocalDate.now()): Long = now.toEpochDay()

    fun dayOf(epochMs: Long): Long =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()

    fun parseTime(hhmm: String): LocalTime? = try {
        val parts = hhmm.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (t: Throwable) {
        null
    }

    fun formatTime(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)

    /** Minutes from [now] until the next occurrence of [hhmm] (today or tomorrow). */
    fun minutesUntil(hhmm: String, now: LocalDateTime = LocalDateTime.now()): Long {
        val time = parseTime(hhmm) ?: return -1
        var target = now.toLocalDate().atTime(time)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return java.time.Duration.between(now, target).toMinutes()
    }

    fun sanitizeTimes(times: List<String>): List<String> =
        times.mapNotNull { parseTime(it)?.let(::formatTime) }.distinct().sorted().take(4)
}
