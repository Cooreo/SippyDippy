package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.system.rememberGravityTilt
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
    /** Fired once when the goal is first reached, for the caller's haptic. */
    onGoalReached: (() -> Unit)? = null,
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

    // B5: the surface leans with the phone. Read inside the draw scope below, so a
    // sensor sample costs a draw, not a recomposition.
    val tilt = rememberGravityTilt(animate)

    // A splash where the water lands, whenever the level goes up (a logged sip).
    val landing = remember { Animatable(1f) }
    var lastMl by remember { mutableIntStateOf(hydratedMl) }
    LaunchedEffect(hydratedMl) {
        if (hydratedMl > lastMl && animate) {
            landing.snapTo(0f)
            landing.animateTo(1f, tween(durationMillis = 720))
        }
        lastMl = hydratedMl
    }
    // The droplet burst when the goal is reached: once per crossing.
    val burst = remember { Animatable(1f) }
    LaunchedEffect(goalReached) {
        if (goalReached && animate) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = 900))
            onGoalReached?.invoke()
        }
    }

    Box(
        modifier = modifier
            .size(diameter)
            // B5: the soft blue reflection the orb sits on.
            .drawBehind {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accent.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height * 0.98f),
                        radius = size.width * 0.46f,
                    ),
                    topLeft = Offset(size.width * 0.14f, size.height * 0.90f),
                    size = Size(size.width * 0.72f, size.height * 0.18f),
                )
            }
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

            // Round 9, 1.1: the vessel. A dark body, so an empty orb reads as a
            // glass with air in it rather than a clear ring floating on the
            // backdrop. Vignetted toward the rim, which is what gives it depth.
            drawCircle(color = c.bg1.copy(alpha = 0.62f), radius = radius)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        c.bg0.copy(alpha = 0.55f),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.glassInner, Color.Transparent),
                    center = Offset(size.width * 0.36f, size.height * 0.26f),
                    radius = radius * 1.1f,
                ),
                radius = radius,
            )
            if (shown <= 0.001f) {
                // At zero there is still water in the glass: a faint resting line
                // near the bottom, so "empty" reads as a level, not as nothing.
                val lineY = size.height * 0.72f
                drawLine(
                    color = c.accentBright.copy(alpha = 0.22f),
                    start = Offset(size.width * 0.18f, lineY),
                    end = Offset(size.width * 0.82f, lineY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            val surfaceY = size.height * (1f - shown)
            val disc = Path().apply {
                addOval(Rect(Offset.Zero, Size(size.width, size.height)))
            }
            val amplitude = if (animate) 7.dp.toPx() else 0f

            clipPath(disc) {
                // B5: the whole body leans with the phone (gravity, low-passed).
                // Two wave layers at different speeds ride on top of that lean.
                rotate(degrees = tilt.floatValue * 0.8f, pivot = center) {
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

                    // Inner glow that grows with the level.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f + 0.16f * shown),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.86f),
                            radius = radius * (0.45f + 0.55f * shown),
                        ),
                        radius = radius,
                        center = Offset(size.width * 0.5f, size.height * 0.86f),
                    )

                    // Above 100%: a slow shimmer swimming across the surface.
                    if (fraction > 1f && animate) {
                        val sweep = ((phase * 2f) % 1f) * size.width * 1.8f - size.width * 0.4f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                                start = Offset(sweep, surfaceY),
                                end = Offset(sweep + size.width * 0.42f, size.height),
                            ),
                            topLeft = Offset(0f, surfaceY),
                            size = Size(size.width, size.height - surfaceY),
                        )
                    }

                    // A splash ring where the water lands after a logged sip.
                    if (animate) {
                        val p = landing.value
                        if (p < 1f) {
                            drawCircle(
                                color = Color.White.copy(alpha = (1f - p) * 0.55f),
                                radius = radius * (0.10f + 0.66f * p),
                                center = Offset(size.width * 0.5f, surfaceY + 4f),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
            }

            // Goal reached: ten droplets thrown up from the surface, then gravity.
            if (animate) {
                val p = burst.value
                if (p < 1f) {
                    repeat(10) { i ->
                        val angle = (195f + i * 15f) * PI.toFloat() / 180f
                        val distance = radius * (0.34f + 1.05f * p)
                        val x = center.x + kotlin.math.cos(angle) * distance
                        val y = center.y + kotlin.math.sin(angle) * distance + radius * 0.9f * p * p
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - p) * 0.72f),
                            radius = (2.6f - 1.5f * p) * density,
                            center = Offset(x, y),
                        )
                    }
                }
            }

            // B5: a specular arc at the top-left, the highlight a glass sphere has.
            drawArc(
                color = Color.White.copy(alpha = 0.30f),
                startAngle = 202f,
                sweepAngle = 54f,
                useCenter = false,
                topLeft = Offset(size.width * 0.13f, size.height * 0.10f),
                size = Size(size.width * 0.60f, size.height * 0.60f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )

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
