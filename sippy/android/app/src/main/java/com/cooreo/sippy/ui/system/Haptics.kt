package com.cooreo.sippy.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The three haptics the app uses (round 8, B5/B10): a tab tick, a log thud and a
 * goal success. Compose's own types only — no new dependency, no vibration
 * permission — and every call is a no-op when the player turned haptics off.
 */
class Haptics(
    private val feedback: HapticFeedback,
    private val enabled: Boolean,
) {
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun thud() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Two beats: the second one is what makes it read as "success". */
    fun success() {
        if (!enabled) return
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean = true): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}
