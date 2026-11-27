package com.cooreo.sippy.domain.water

import com.cooreo.sippy.model.LogEntry
import kotlin.math.roundToInt

/** Pure goal / hydration maths. No Android types — unit tested on the JVM. */
object WaterMath {

    /** Hydrated millilitres for a volume of a given drink. */
    fun hydrate(volumeMl: Int, hydrationFactor: Float): Int {
        if (volumeMl <= 0) return 0
        val factor = hydrationFactor.coerceIn(0f, 1f)
        return (volumeMl * factor).roundToInt().coerceAtLeast(0)
    }

    /** Today's credited millilitres. */
    fun hydratedMl(entries: List<LogEntry>): Int = entries.sumOf { it.hydratedMl }.coerceAtLeast(0)

    /** Ring fraction, always inside 0..1. */
    fun goalFraction(hydratedMl: Int, goalMl: Int): Float {
        if (goalMl <= 0) return 0f
        return (hydratedMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    }

    fun goalMet(hydratedMl: Int, goalMl: Int): Boolean = goalMl > 0 && hydratedMl >= goalMl

    fun remainingMl(hydratedMl: Int, goalMl: Int): Int =
        (goalMl - hydratedMl).coerceAtLeast(0)

    /** Goal clamp used by Settings (750..3000 ml). */
    fun clampGoal(goalMl: Int, min: Int = 750, max: Int = 3000): Int =
        goalMl.coerceIn(min, max)
}
