package com.cooreo.sippy.ui.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.ui.theme.LocalSippyColors
import kotlin.math.roundToInt

/**
 * Everything that happens between confirming the Log sheet and the water settling
 * in the orb (round 8, B7): a droplet arcs from the button into the orb along a
 * parabola, and "+330 ml" and "+1 pt" rise from the orb and fade.
 *
 * One animation drives all of it, and it is drawn in a single full-screen overlay:
 * nothing here recomposes a screen, and the whole thing is 760 ms — a droplet in
 * flight is the only part allowed to outlive the 700 ms the flow itself is held to,
 * because by then the drink is already logged.
 */
data class LogFlyData(
    val id: Long,
    val from: Rect,
    val to: Rect,
    val volumeMl: Int,
    val points: Int,
    val tint: Color,
)

@Composable
fun LogFly(
    data: LogFlyData,
    animate: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalSippyColors.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data.id) {
        if (animate) {
            progress.animateTo(1f, tween(760, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
        onFinished()
    }

    val from = if (data.from == Rect.Zero) data.to else data.from
    val target = if (data.to == Rect.Zero) from else data.to

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            // Flight ends at 60% of the timeline; the rest is the labels fading.
            if (p < 0.62f) {
                val t = (p / 0.62f).coerceIn(0f, 1f)
                val start = Offset(from.center.x, from.center.y)
                val end = Offset(target.center.x, target.center.y)
                // Control point above the midpoint: a droplet arcs, it does not fall.
                val control = Offset(
                    x = (start.x + end.x) / 2f,
                    y = minOf(start.y, end.y) - size.height * 0.10f,
                )
                val inv = 1f - t
                val point = Offset(
                    x = inv * inv * start.x + 2f * inv * t * control.x + t * t * end.x,
                    y = inv * inv * start.y + 2f * inv * t * control.y + t * t * end.y,
                )
                val radius = size.minDimension * 0.014f * (1f - 0.35f * t)
                drawCircle(color = data.tint.copy(alpha = 0.95f), radius = radius, center = point)
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = radius * 0.38f,
                    center = Offset(point.x - radius * 0.3f, point.y - radius * 0.3f),
                )
            }
        }

        // The labels rise from the orb and fade out just before the end.
        val p = progress.value.coerceIn(0f, 1f)
        val rise = 34.dp.toPx() * p
        val alpha = if (p < 0.15f) p / 0.15f else (1f - (p - 0.15f) / 0.85f).coerceIn(0f, 1f)
        val baseX = target.center.x
        val baseY = target.top - 18.dp.toPx()

        Text(
            text = "+${data.volumeMl} ml",
            color = c.accentBright,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (baseX - 30.dp.toPx()).roundToInt(),
                        y = (baseY - rise).roundToInt(),
                    )
                }
                .graphicsLayer { this.alpha = alpha },
        )
        if (data.points > 0) {
            Text(
                text = "+${data.points} pt",
                color = c.ok,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (baseX + 6.dp.toPx()).roundToInt(),
                            y = (baseY - rise - 22.dp.toPx()).roundToInt(),
                        )
                    }
                    .graphicsLayer { this.alpha = alpha * 0.9f },
            )
        }
    }
}
