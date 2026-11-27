package com.cooreo.sippy

import com.cooreo.sippy.domain.log.LogGroups
import com.cooreo.sippy.model.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogGroupsTest {

    private fun sip(id: String, day: Long, minutes: Long, volume: Int, counted: Int) = LogEntry(
        id = id,
        dayEpoch = day,
        atEpochMs = day * 86_400_000L + minutes * 60_000L,
        drinkId = "water",
        volumeMl = volume,
        hydratedMl = counted,
        sugary = false,
    )

    private val entries = listOf(
        sip("a", 100, 480, 250, 250),
        sip("b", 100, 900, 500, 500),
        sip("c", 101, 420, 330, 297),
        sip("d", 101, 780, 250, 250),
        sip("e", 99, 600, 250, 250),
    )

    @Test
    fun daysAreNewestFirstAndEntriesInsideADayAreNewestFirst() {
        val groups = LogGroups.byDay(entries, todayEpoch = 101)
        assertEquals(listOf(101L, 100L, 99L), groups.map { it.dayEpoch })
        assertEquals(listOf("d", "c"), groups[0].entries.map { it.id })
        assertEquals(listOf("b", "a"), groups[1].entries.map { it.id })
    }

    @Test
    fun labelsAreTodayYesterdayThenTheDate() {
        assertEquals("Today", LogGroups.labelFor(101, todayEpoch = 101))
        assertEquals("Yesterday", LogGroups.labelFor(100, todayEpoch = 101))
        val older = LogGroups.labelFor(97, todayEpoch = 101)
        assertTrue("expected a date, was '$older'", older.isNotBlank())
        assertTrue("expected a day number, was '$older'", older.any(Char::isDigit))
        assertTrue(older != "Today" && older != "Yesterday")
    }

    @Test
    fun eachDayCarriesItsOwnTotals() {
        val groups = LogGroups.byDay(entries, todayEpoch = 101)
        assertEquals(580, groups[0].volumeMl)
        assertEquals(547, groups[0].countedMl)
        assertEquals(750, groups[1].volumeMl)
        assertEquals(750, groups[1].countedMl)
    }

    @Test
    fun homeSeesOnlyTheNewestThreeOfToday() {
        val today = (1..5).map { sip("t$it", 101, it * 60L, 250, 250) }
        val newest = LogGroups.newestOfDay(today + entries, todayEpoch = 101, limit = 3)
        assertEquals(listOf("t5", "t4", "t3"), newest.map { it.id })
    }

    @Test
    fun anEmptyLogHasNoGroups() {
        assertTrue(LogGroups.byDay(emptyList(), todayEpoch = 101).isEmpty())
        assertTrue(LogGroups.newestOfDay(emptyList(), 101, 3).isEmpty())
    }
}
