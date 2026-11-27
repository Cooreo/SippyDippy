package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * Goal-reached celebration: a short burst of droplets and a caption.
 * Collapsed entirely when animations are off (system setting or Reduce
 * effects) — [animate] false means nothing is drawn at all.
 */
@Composable
fun Celebration(
    trigger: Long,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (trigger == 0L || !animate) return
    val c = LocalSippyColors.current
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = 1700, easing = LinearEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            if (t >= 1f) return@Canvas
            val count = 22
            for (i in 0 until count) {
                val seed = (i * 37 % 100) / 100f
                val delay = (i % 7) / 9f
                val local = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val x = size.width * (0.08f + seed * 0.84f)
                val startY = size.height * 0.42f
                val y = startY + (size.height * 0.6f) * local * local
                val r = (size.minDimension * 0.012f) * (1f - local * 0.4f)
                val alpha = (1f - local).coerceIn(0f, 1f)
                val color = if (i % 3 == 0) c.accentBright else c.accent
                drawRoundRect(
                    color = color.copy(alpha = alpha * 0.9f),
                    topLeft = Offset(x - r, y - r),
                    size = Size(r * 2f, r * 2.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                )
            }
        }
        Text(
            text = "Goal reached",
            color = c.text1,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
