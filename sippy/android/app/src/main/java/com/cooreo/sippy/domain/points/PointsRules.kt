package com.cooreo.sippy.domain.points

import com.cooreo.sippy.model.PointsEntry

/**
 * Points (round 7, B4). One point per 250 ml of counted hydration, with the
 * remainder carried to the next entry — so logging 200 ml twice earns one
 * point, not zero. Pure, so the earning table, the carry and the "never
 * negative" rule are all unit tested.
 */
object PointsRules {

    const val STEP_ML = 250

    /** Newest-last ledger cap. */
    const val LEDGER_LIMIT = 500

    data class Credit(
        /** Balance after crediting. */
        val points: Int,
        /** Millilitres carried toward the next point. */
        val carryMl: Int,
        /** Points earned by this entry alone. */
        val gained: Int,
    )

    fun credit(points: Int, carryMl: Int, countedMl: Int): Credit {
        val safePoints = points.coerceAtLeast(0)
        val total = carryMl.coerceAtLeast(0) + countedMl.coerceAtLeast(0)
        val gained = total / STEP_ML
        return Credit(
            points = safePoints + gained,
            carryMl = total % STEP_ML,
            gained = gained,
        )
    }

    /** Spends or claws back, never below zero. */
    fun debit(balance: Int, amount: Int): Int =
        (balance - amount.coerceAtLeast(0)).coerceAtLeast(0)

    /** Appends one ledger line, dropping the oldest beyond [LEDGER_LIMIT]. */
    fun append(ledger: List<PointsEntry>, entry: PointsEntry): List<PointsEntry> =
        (ledger + entry).takeLast(LEDGER_LIMIT)

    /** How far the current balance is from the next point, in millilitres. */
    fun millilitresToNextPoint(carryMl: Int): Int =
        STEP_ML - carryMl.coerceIn(0, STEP_ML - 1)
}
