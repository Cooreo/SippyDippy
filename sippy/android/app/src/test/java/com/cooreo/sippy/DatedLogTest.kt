package com.cooreo.sippy

import com.cooreo.sippy.domain.log.LogGroups
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Round 8, A3: the full dated log. The cases the brief names are all here —
 * 200 entries over 40 days, the local-midnight split, a DST change, and
 * identical timestamps keeping a stable order.
 */
class DatedLogTest {

    private val warsaw: ZoneId = ZoneId.of("Europe/Warsaw")

    private fun entry(
        id: String,
        dayEpoch: Long,
        atEpochMs: Long,
        volumeMl: Int = 250,
        countedMl: Int = 250,
    ) = LogEntry(
        id = id,
        dayEpoch = dayEpoch,
        atEpochMs = atEpochMs,
        drinkId = "water",
        volumeMl = volumeMl,
        hydratedMl = countedMl,
        sugary = false,
    )

    private fun ms(date: String, hour: Int, minute: Int): Long =
        LocalDate.parse(date).atTime(hour, minute).atZone(warsaw).toInstant().toEpochMilli()

    @Test
    fun twoHundredEntriesOverFortyDaysSortNewestFirst() {
        val start = LocalDate.parse("2026-10-01")
        val entries = buildList {
            for (day in 0 until 40) {
                val date = start.plusDays(day.toLong())
                for (i in 0 until 5) {
                    val at = date.atTime(8 + i, 30).atZone(warsaw).toInstant().toEpochMilli()
                    add(entry("d${day}e$i", date.toEpochDay(), at))
                }
            }
        }
        assertEquals(200, entries.size)

        val groups = LogGroups.byDay(entries, todayEpoch = start.plusDays(39).toEpochDay())

        assertEquals(40, groups.size)
        // Days newest first.
        assertEquals(
            groups.map { it.dayEpoch }.sortedDescending(),
            groups.map { it.dayEpoch },
        )
        // Inside a day, newest first.
        groups.forEach { group ->
            assertEquals(5, group.entries.size)
            assertEquals(
                group.entries.map { it.atEpochMs }.sortedDescending(),
                group.entries.map { it.atEpochMs },
            )
        }
        assertEquals(200 * 250, groups.sumOf { it.countedMl })
    }

    @Test
    fun localMidnightSplitsTwoEntriesOneMinuteApart() {
        val lateMs = ms("2026-11-20", 23, 59)
        val earlyMs = ms("2026-11-21", 0, 0)

        val lateDay = Dates.dayOf(lateMs, warsaw)
        val earlyDay = Dates.dayOf(earlyMs, warsaw)

        assertEquals(earlyDay, lateDay + 1)
        assertTrue("one minute apart, different days", earlyMs - lateMs == 60_000L)

        val groups = LogGroups.byDay(
            listOf(
                entry("late", lateDay, lateMs),
                entry("early", earlyDay, earlyMs),
            ),
            todayEpoch = earlyDay,
        )
        assertEquals(listOf("Today", "Yesterday"), groups.map { it.label })
        assertEquals(2, groups.size)
    }

    @Test
    fun everyEntryAroundTheDstChangeLandsOnTheRightDay() {
        // Europe/Warsaw springs forward at 02:00 on 2026-03-29.
        val beforeChange = ms("2026-03-29", 1, 30)
        val afterChange = ms("2026-03-29", 3, 30)
        val day = Dates.dayOf(afterChange, warsaw)

        assertEquals(day, Dates.dayOf(beforeChange, warsaw))
        assertEquals(day + 1, Dates.dayOf(ms("2026-03-30", 0, 30), warsaw))
        assertEquals(day - 1, Dates.dayOf(ms("2026-03-28", 23, 59), warsaw))
        // The clocks moved, so two wall-clock hours are only one real hour apart.
        assertEquals(60 * 60_000L, afterChange - beforeChange)

        val groups = LogGroups.byDay(
            listOf(
                entry("before", Dates.dayOf(beforeChange, warsaw), beforeChange),
                entry("after", Dates.dayOf(afterChange, warsaw), afterChange),
            ),
            todayEpoch = day,
        )
        assertEquals(1, groups.size)
        assertEquals(listOf("after", "before"), groups[0].entries.map { it.id })
    }

    @Test
    fun identicalTimestampsKeepAStableOrder() {
        val at = ms("2026-11-22", 9, 15)
        val day = Dates.dayOf(at, warsaw)
        val a = entry("a", day, at)
        val b = entry("b", day, at)
        val c = entry("c", day, at)

        val one = LogGroups.byDay(listOf(a, b, c), todayEpoch = day)[0].entries.map { it.id }
        val two = LogGroups.byDay(listOf(c, a, b), todayEpoch = day)[0].entries.map { it.id }
        val three = LogGroups.byDay(listOf(b, c, a), todayEpoch = day)[0].entries.map { it.id }

        assertEquals(one, two)
        assertEquals(one, three)
        // Deterministic rule: the id breaks the tie, descending.
        assertEquals(listOf("c", "b", "a"), one)
    }

    @Test
    fun otherYearsCarryTheirYearAndThisYearDoesNot() {
        val today = LocalDate.parse("2026-11-27")
        val thisYear = LogGroups.labelFor(LocalDate.parse("2026-11-24").toEpochDay(), today.toEpochDay())
        val lastYear = LogGroups.labelFor(LocalDate.parse("2025-11-24").toEpochDay(), today.toEpochDay())

        assertTrue("this year: '$thisYear'", thisYear.contains("24"))
        assertFalse("this year keeps the label short: '$thisYear'", thisYear.contains("2026"))
        assertTrue("other year names it: '$lastYear'", lastYear.contains("2025"))
        assertNotEquals(thisYear, lastYear)
    }

    @Test
    fun eachDayKnowsWhetherItMetTheGoal() {
        val day = LocalDate.parse("2026-11-23").toEpochDay()
        val over = listOf(entry("x", day, ms("2026-11-23", 9, 0), 500, 500), entry("y", day, ms("2026-11-23", 12, 0), 600, 600))
        val under = listOf(entry("z", day, ms("2026-11-23", 9, 0), 400, 400))

        assertTrue(LogGroups.byDay(over, day, goalMl = 1000)[0].goalMet)
        assertFalse(LogGroups.byDay(under, day, goalMl = 1000)[0].goalMet)
        // No goal set: nothing can be marked as met.
        assertFalse(LogGroups.byDay(over, day, goalMl = 0)[0].goalMet)
        assertEquals(1100, LogGroups.byDay(over, day, goalMl = 1000)[0].volumeMl)
    }
}
