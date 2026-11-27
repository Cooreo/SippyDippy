package com.cooreo.sippy.ui.system

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether motion is allowed right now. The shell provides the whole answer
 * (the user's Reduce effects setting AND the system animator scale), so every
 * control below asks one question instead of three.
 */
val LocalMotionAllowed = staticCompositionLocalOf { true }

/** Reads `Settings.Global.ANIMATOR_DURATION_SCALE`; 0 means animations are off. */
fun systemAnimationsDisabled(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f
} catch (t: Throwable) {
    false
}

@Composable
fun motionAllowed(): Boolean =
    LocalMotionAllowed.current && !systemAnimationsDisabled(LocalContext.current)
