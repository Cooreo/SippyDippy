package com.cooreo.sippy.ui.system

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Immersive-sticky system bars.
 *
 * Sippy is one full-screen scene, so the status and navigation bars are hidden;
 * a swipe from an edge brings them back as a transient overlay
 * ([WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE]) and
 * they fade out again. Because the bars can appear at any moment, every screen
 * still pads itself by the *stable* insets (see `WindowInsets.statusBarsIgnoringVisibility`
 * in `SippyRoot` and the sheet scrim), so content never sits under the clock or
 * the gesture bar in either state.
 */
object SystemBars {

    /** Hides both bars and asks for the transient-by-swipe behaviour. */
    fun hideImmersive(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    /** Restores the bars (used when the activity goes away). */
    fun show(activity: Activity) {
        val window = activity.window
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }
}
