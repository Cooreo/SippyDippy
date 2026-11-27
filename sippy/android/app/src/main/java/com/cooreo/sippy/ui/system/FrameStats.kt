package com.cooreo.sippy.ui.system

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Frames per second and the worst frame in the last second (round 8, B10).
 *
 * Measured with the Choreographer, so it is the real display cadence, not an
 * estimate. It only runs while something reads it — the Developer screen — and
 * the callback is removed with the composition: a diagnostic must never cost the
 * frames it is measuring.
 */
data class FrameStats(
    val fps: Int = 0,
    val worstFrameMs: Float = 0f,
    val frames: Int = 0,
)

@Composable
fun rememberFrameStats(enabled: Boolean): FrameStats {
    var stats by remember { mutableStateOf(FrameStats()) }
    if (!enabled) return stats
    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        var running = true
        var lastFrame = 0L
        var windowStart = 0L
        var frames = 0
        var worst = 0f
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                if (lastFrame == 0L) {
                    lastFrame = frameTimeNanos
                    windowStart = frameTimeNanos
                } else {
                    val deltaMs = (frameTimeNanos - lastFrame) / 1_000_000f
                    frames += 1
                    if (deltaMs > worst) worst = deltaMs
                    val elapsed = frameTimeNanos - windowStart
                    if (elapsed >= 1_000_000_000L) {
                        stats = FrameStats(
                            fps = (frames * 1_000_000_000.0 / elapsed).toInt(),
                            worstFrameMs = worst,
                            frames = frames,
                        )
                        windowStart = frameTimeNanos
                        frames = 0
                        worst = 0f
                    }
                    lastFrame = frameTimeNanos
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose {
            running = false
            choreographer.removeFrameCallback(callback)
        }
    }
    return stats
}
