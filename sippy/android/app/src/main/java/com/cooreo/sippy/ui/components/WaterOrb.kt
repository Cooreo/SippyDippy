package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The website's water orb, rebuilt in Compose (SPEC §2).
 *
 * One progress value drives everything: the surface height is `1 - fraction` of
 * the disc, the two wave layers are the same path translated horizontally by
 * the animation phase (transform, never layout), bubbles move by translation and
 * opacity only, and the splash ring is scale + opacity. Under reduce effects the
 * orb renders a static fill at the current percentage: no waves, no bubbles, no
 * splash, no animation loop.
 */
@Composable
fun WaterOrb(
    hydratedMl: Int,
    goalMl: Int,
    fraction: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = 260.dp,
) {
    val c = LocalSippyColors.current
    val target = fraction.coerceIn(0f, 1f)
    val level by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) {
            spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessLow)
        } else {
            tween(durationMillis = 0)
        },
        label = "orbLevel",
    )
    val shown = if (animate) level else target

    val phase = if (animate) {
        rememberInfiniteTransition(label = "orbWaves").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbPhase",
        ).value
    } else {
        0f
    }

    // Eight bubbles at most (SPEC): x, radius, speed, start offset, alpha.
    val bubbles = remember {
        List(8) { i ->
            val seed = (i * 37 % 100) / 100f
            Bubble(
                x = 0.14f + seed * 0.72f,
                radius = 2.2f + (i % 3),
                speed = 0.55f + (i % 4) * 0.16f,
                offset = seed,
                alpha = 0.30f + (i % 3) * 0.10f,
            )
        }
    }

    val goalReached = target >= 1f
    val percent = (shown * 100).roundToInt()

    Box(
        modifier = modifier
            .size(diameter)
            .semantics {
                contentDescription = if (goalReached) {
                    "Goal reached: $hydratedMl of $goalMl millilitres"
                } else {
                    "$hydratedMl of $goalMl millilitres, $percent percent of today's goal"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Glass well behind the water.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.glassInner, c.glassTint.copy(alpha = 0.18f)),
                    center = Offset(size.width * 0.36f, size.height * 0.28f),
                    radius = radius * 1.25f,
                ),
                radius = radius,
            )

            val surfaceY = size.height * (1f - shown)
            val disc = Path().apply {
                addOval(Rect(Offset.Zero, Size(size.width, size.height)))
            }
            val amplitude = if (animate) 7.dp.toPx() else 0f

            clipPath(disc) {
                drawWave(
                    width = size.width,
                    height = size.height,
                    surfaceY = surfaceY,
                    amplitude = amplitude,
                    phase = phase,
                    layer = 0f,
                    top = c.accentBright.copy(alpha = 0.80f),
                    bottom = c.accentDeep.copy(alpha = 0.96f),
                )
                drawWave(
                    width = size.width,
                    height = size.height,
                    surfaceY = surfaceY,
                    amplitude = amplitude * 0.6f,
                    phase = phase,
                    layer = 0.34f,
                    top = Color.White.copy(alpha = 0.22f),
                    bottom = Color.Transparent,
                )

                if (animate) {
                    bubbles.forEach { bubble ->
                        val travel = ((phase * bubble.speed + bubble.offset) % 1f)
                        val y = size.height - travel * (size.height - surfaceY)
                        if (y > surfaceY + 2f) {
                            val fade = (1f - travel).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color.White.copy(alpha = bubble.alpha * fade),
                                radius = bubble.radius.dp.toPx(),
                                center = Offset(size.width * bubble.x, y),
                            )
                        }
                    }
                }
            }

            // Rim highlight, drawn outside the clip so it stays crisp.
            drawCircle(
                color = c.glassRimStart.copy(alpha = 0.42f),
                radius = radius - 1.dp.toPx() / 2f,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Goal reached: a splash ring that scales and fades (transform+opacity).
            if (goalReached && animate) {
                val splash = (phase * 1.8f) % 1f
                drawCircle(
                    color = c.accentBright.copy(alpha = (1f - splash) * 0.35f),
                    radius = radius * (0.90f + 0.16f * splash),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = hydratedMl.toString(),
                style = SippyType.value,
                color = c.text1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "of $goalMl ml",
                style = SippyType.caption,
                color = c.text2,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (goalReached) "Goal reached" else "$percent%",
                style = SippyType.label,
                color = if (goalReached) c.ok else c.accentBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private data class Bubble(
    val x: Float,
    val radius: Float,
    val speed: Float,
    val offset: Float,
    val alpha: Float,
)

/**
 * One wave body: a flat top when [amplitude] is zero (reduce effects), a sine
 * surface otherwise. [layer] shifts the wave horizontally, which is a pure
 * translation of the same shape.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    width: Float,
    height: Float,
    surfaceY: Float,
    amplitude: Float,
    phase: Float,
    layer: Float,
    top: Color,
    bottom: Color,
) {
    val path = Path().apply {
        moveTo(0f, surfaceY)
        if (amplitude > 0f) {
            val stepPx = width / 48f
            var x = 0f
            while (x < width) {
                val angle = (x / width * 2f + phase + layer) * 2f * PI.toFloat()
                lineTo(x, surfaceY + sin(angle) * amplitude)
                x += stepPx
            }
            val endAngle = (2f + phase + layer) * 2f * PI.toFloat()
            lineTo(width, surfaceY + sin(endAngle) * amplitude)
        } else {
            lineTo(width, surfaceY)
        }
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(top, bottom),
            startY = surfaceY,
            endY = height,
        ),
    )
}
