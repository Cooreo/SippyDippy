package com.cooreo.sippy.ui.system

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * Immersive-sticky system bars.
 *
 * Sippy is one full-screen scene, so the status and navigation bars are hidden;
 * a swipe from an edge brings them back as a transient overlay and they fade out
 * again. Because the bars can reappear at any moment, every screen still pads
 * itself by the *stable* insets (`WindowInsets.statusBarsIgnoringVisibility` in
 * `SippyRoot`, `navigationBarsPadding()` in the sheet scrim), so content never
 * sits under the clock or the gesture bar in either state.
 *
 * Deliberately platform-only (no androidx.core inset-controller calls): the API
 * 30+ path is `WindowInsetsController`, the API 26–29 path is the legacy
 * systemUiVisibility flags, and both are the documented immersive recipes.
 */
object SystemBars {

    /** Hides both bars and asks for the transient-by-swipe behaviour. */
    fun hideImmersive(activity: Activity) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }
}
