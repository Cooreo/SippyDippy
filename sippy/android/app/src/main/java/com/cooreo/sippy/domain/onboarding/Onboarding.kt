package com.cooreo.sippy.domain.onboarding

import com.cooreo.sippy.model.Settings
import kotlin.math.roundToInt

/**
 * Pure onboarding rules — no Android types, so the restore/clamp behaviour is
 * unit tested on the JVM. Three pages: welcome, goal, first friend.
 */
object Onboarding {
    const val PAGE_COUNT = 3
    const val PAGE_WELCOME = 0
    const val PAGE_GOAL = 1
    const val PAGE_FRIEND = 2

    /** Slider granularity for the goal page (ml). */
    const val GOAL_STEP_ML = 50

    /** Chips offered on the goal page; all inside the allowed range. */
    val GOAL_PRESETS: List<Int> = listOf(1500, 2000, 2500)

    /** Steps count material3 needs for a 750..3000 slider at [GOAL_STEP_ML]. */
    val GOAL_SLIDER_STEPS: Int =
        ((Settings.MAX_GOAL_ML - Settings.MIN_GOAL_ML) / GOAL_STEP_ML) - 1

    fun clampPage(page: Int): Int = page.coerceIn(0, PAGE_COUNT - 1)

    /**
     * The page to open on: the stored page, clamped, or null once onboarding is
     * finished. A stored page left over from a finished run is ignored.
     */
    fun startPage(storedPage: Int, complete: Boolean): Int? =
        if (complete) null else clampPage(storedPage)

    fun isLastPage(page: Int): Boolean = clampPage(page) == PAGE_FRIEND

    fun nextPage(page: Int): Int = clampPage(clampPage(page) + 1)

    /** Slider value -> whole ml, snapped to [GOAL_STEP_ML] and inside the range. */
    fun snapGoal(ml: Float): Int {
        val snapped = (ml / GOAL_STEP_ML).roundToInt() * GOAL_STEP_ML
        return snapped.coerceIn(Settings.MIN_GOAL_ML, Settings.MAX_GOAL_ML)
    }

    /** Existing (possibly older) settings goal -> a valid, snapped goal. */
    fun goalFor(goalMl: Int): Int = snapGoal(goalMl.toFloat())

    /** Progress 0..1 for a goal, used by the pager-visible slider track. */
    fun goalProgress(goalMl: Int): Float {
        val span = (Settings.MAX_GOAL_ML - Settings.MIN_GOAL_ML).toFloat()
        return ((goalFor(goalMl) - Settings.MIN_GOAL_ML) / span).coerceIn(0f, 1f)
    }
}
