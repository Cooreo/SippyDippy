package com.cooreo.sippy

import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.model.PointsEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointsRulesTest {

    private fun entry(i: Int) = PointsEntry(
        id = "e$i",
        atEpochMs = i.toLong(),
        delta = 1,
        reason = "Logged 250 ml",
        balanceAfter = i,
    )

    @Test
    fun onePointPerTwoHundredAndFiftyMillilitres() {
        assertEquals(1, PointsRules.credit(0, 0, 250).gained)
        assertEquals(2, PointsRules.credit(0, 0, 500).gained)
        assertEquals(0, PointsRules.credit(0, 0, 249).gained)
        assertEquals(4, PointsRules.credit(0, 0, 1_000).gained)
    }

    @Test
    fun theRemainderCarriesToTheNextEntry() {
        val first = PointsRules.credit(0, 0, 200)
        assertEquals(0, first.gained)
        assertEquals(200, first.carryMl)

        val second = PointsRules.credit(first.points, first.carryMl, 100)
        assertEquals(1, second.gained)
        assertEquals(50, second.carryMl)
        assertEquals(1, second.points)
    }

    @Test
    fun carryNeverReachesAWholePoint() {
        var points = 0
        var carry = 0
        listOf(90, 90, 90, 60, 10, 240, 5).forEach { ml ->
            val credit = PointsRules.credit(points, carry, ml)
            points = credit.points
            carry = credit.carryMl
            assertTrue("carry $carry", carry in 0 until PointsRules.STEP_ML)
        }
        assertEquals(2, points) // 585 ml total → 2 points, 85 ml carried
        assertEquals(85, carry)
    }

    @Test
    fun theBalanceNeverGoesNegative() {
        assertEquals(0, PointsRules.debit(0, 30))
        assertEquals(0, PointsRules.debit(10, 30))
        assertEquals(5, PointsRules.debit(10, 5))
        // A negative amount is not a refund: it debits nothing.
        assertEquals(3, PointsRules.debit(3, -100))
        assertEquals(0, PointsRules.credit(-40, -10, -100).points)
    }

    @Test
    fun theLedgerKeepsTheNewestFiveHundred() {
        var ledger = emptyList<PointsEntry>()
        repeat(505) { ledger = PointsRules.append(ledger, entry(it)) }
        assertEquals(PointsRules.LEDGER_LIMIT, ledger.size)
        assertEquals("e5", ledger.first().id)
        assertEquals("e504", ledger.last().id)
    }

    @Test
    fun theNextPointDistanceMatchesTheCarry() {
        assertEquals(250, PointsRules.millilitresToNextPoint(0))
        assertEquals(50, PointsRules.millilitresToNextPoint(200))
        assertEquals(1, PointsRules.millilitresToNextPoint(249))
        // A stale carry of a whole step is clamped into range, so the distance
        // to the next point can never be zero or negative.
        assertEquals(1, PointsRules.millilitresToNextPoint(250))
        assertEquals(250, PointsRules.millilitresToNextPoint(-40))
    }
}
