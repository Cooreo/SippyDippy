package com.cooreo.sippy.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.cooreo.sippy.R
import com.cooreo.sippy.ui.system.LocalMotionAllowed
import kotlin.math.PI
import kotlin.math.sin

/**
 * Which tab is on top, so the root backdrop can tint itself (round 8, B2). The
 * host owns one of these and provides it; a screen only writes [color].
 */
class BackdropTint {
    var color by mutableStateOf(Color.Transparent)
}

val LocalBackdropTint = staticCompositionLocalOf { BackdropTint() }

/** The four tints. Deep blue, teal, violet, navy — one per tab. */
object BackdropTints {
    val home = Color(0xFF0B3A6B)
    val drinks = Color(0xFF0C6B62)
    val friends = Color(0xFF35256B)
    val settings = Color(0xFF0E1D38)
}

/**
 * The backdrop (B2): one generated portrait image of the water in the site's
 * palette — aqua at the top with light shafts, near-black navy at the bottom —
 * then two slow shafts, twelve bubbles drifting up, the current tab's tint and a
 * top scrim so white type stays readable over the bright water.
 *
 * Only translation and alpha move. Everything pauses when the app is not
 * resumed or when Reduce effects is on.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val tintState = LocalBackdropTint.current
    // Crossfade between the outgoing and incoming tab tint. Animatable + lerp
    // instead of animateColorAsState keeps this to two core animation imports.
    var from by remember { mutableStateOf(tintState.color) }
    var to by remember { mutableStateOf(tintState.color) }
    val blend = remember { Animatable(1f) }
    LaunchedEffect(tintState.color) {
        val target = tintState.color
        if (target != to) {
            from = to
            to = target
            blend.snapTo(0f)
            blend.animateTo(1f, tween(durationMillis = 420))
        }
    }
    val tint: Color = lerp(from, to, blend.value)
    val live = rememberResumed() && LocalMotionAllowed.current

    val clock = rememberInfiniteTransition(label = "backdrop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
        ),
        label = "backdropClock",
    )
    var frozen by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(live) {
        if (!live) frozen = clock.value
    }
    val t = if (live) clock.value else frozen

    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (tint != Color.Transparent) {
            Box(Modifier.fillMaxSize().background(tint.copy(alpha = 0.26f)))
        }
        // Two shafts, each drifting on its own phase; transform and alpha only.
        Canvas(Modifier.fillMaxSize()) {
            val bottom = size.height * 0.86f
            repeat(2) { i ->
                val phase = (t + i * 0.5f) % 1f
                val drift = sin(phase * 2f * PI.toFloat()) * size.width * 0.05f
                val x = size.width * (0.16f + i * 0.42f) + drift
                val topWidth = size.width * 0.10f
                val bottomWidth = size.width * 0.26f
                val path = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + topWidth, 0f)
                    lineTo(x + bottomWidth, bottom)
                    lineTo(x + bottomWidth - topWidth * 1.6f, bottom)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.055f + 0.02f * (1f - phase)),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = bottom,
                    ),
                )
            }
            // Twelve bubbles, each on its own phase and speed.
            repeat(12) { i ->
                val phase = (i * 0.137f) % 1f
                val x = size.width * (0.04f + (i * 0.618f) % 0.92f)
                val radius = (1.6f + (i % 5) * 1.5f) * density
                val progress = (t * (0.6f + (i % 3) * 0.22f) + phase) % 1f
                val y = size.height * (1.04f - progress * 1.12f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f + 0.10f * (1f - progress)),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
        // A soft top scrim: keeps the clock and the greeting readable over the
        // bright water without flattening the image.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.38f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.22f),
                    ),
                ),
        )
    }
}

/** True while the app is actually on screen. */
@Composable
private fun rememberResumed(): Boolean {
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(true) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> resumed = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return resumed
}
