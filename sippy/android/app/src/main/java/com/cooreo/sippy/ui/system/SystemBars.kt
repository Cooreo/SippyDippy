package com.cooreo.sippy.ui.system

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * Visible, transparent system bars over the backdrop (round 8, A2).
 *
 * Round 7 hid both bars and padded the content by `statusBarsPadding()` — which
 * is 0 while they are hidden — so the top row went under the clock as soon as a
 * swipe brought them back. The bar is no longer hidden: it is transparent, the
 * backdrop draws behind it, the icons are light, and the content frame pads by
 * the real status-bar inset (which also covers the display cutout, because the
 * cutout sits inside the status-bar area under the default cutout mode).
 *
 * `setDecorFitsSystemWindows(false)` keeps the edge-to-edge draw; showing the bars
 * (rather than hiding them) is the whole difference from round 7.
 */
object SystemBars {

    /** Transparent bars, light icons, shown. Safe to call repeatedly. */
    fun showTransparent(activity: Activity) {
        val window = activity.window
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                show(WindowInsets.Type.systemBars())
                // 0 = light icons on dark glass. BEHAVIOR_DEFAULT: no immersive
                // swipe-to-hide, no transient overlay bars.
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    )
            }
        }
    }
}
