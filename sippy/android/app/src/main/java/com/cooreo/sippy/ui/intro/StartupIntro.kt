package com.cooreo.sippy.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.intro.Vignette
import com.cooreo.sippy.domain.intro.VignetteMotion
import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.StartupAnimation
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FriendAvatar
import com.cooreo.sippy.ui.theme.SippyType

/** The dark blue the splash screen paints, so the first frame matches this one. */
private val IntroDeep = Color(0xFF0B1F38)

/**
 * The startup animation (round 8, B8): one friend, wearing what it is wearing,
 * performs a vignette in front of the mark, then everything rises and fades
 * straight into Home. It is drawn *over* Home, so nothing ever goes black.
 *
 * Hard cap 2.4s (900ms in Short), any tap skips it, and under Reduce effects it is
 * a 400ms crossfade with the vignette held at its most legible moment.
 */
@Composable
fun StartupIntro(
    state: StoredState,
    vignette: Vignette,
    animate: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = state.settings.startupAnimation
    val totalMs = when (mode) {
        StartupAnimation.FULL -> 2400
        StartupAnimation.SHORT -> 900
        StartupAnimation.OFF -> 0
    }
    if (totalMs == 0) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(vignette, animate, mode) {
        if (animate) {
            progress.animateTo(1f, tween(totalMs, easing = LinearEasing))
        } else {
            // Reduce effects: one crossfade, no flight.
            progress.animateTo(1f, tween(400))
        }
        onFinished()
    }

    val friend = remember(state.friends) { state.friends.firstOrNull() ?: Friend.dippy() }
    val worn = remember(state.equipped, state.equippedOutfits, state.ownedItems, friend.id) {
        ShopRules.worn(state.equipped, state.equippedOutfits, state.ownedItems, friend.id)
    }
    val hat = worn.firstOrNull { it.slot == ItemSlot.HAT }
    val outfit = worn.firstOrNull { it.slot == ItemSlot.OUTFIT }
    val held = remember(vignette) { VignetteMotion.of(vignette, 0.65f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF124B7A), IntroDeep, Color(0xFF06121F))),
            )
            // A tap anywhere skips: removing this overlay is the same code path the
            // end of the animation takes, so there is nothing to unwind.
            .pointerInput(Unit) { detectTapGestures { onFinished() } },
        contentAlignment = Alignment.Center,
    ) {
        val p = progress.value
        // The last 18% hands over to Home: everything rises and fades.
        val exit = ((p - 0.82f) / 0.18f).coerceIn(0f, 1f)

        Canvas(Modifier.fillMaxSize()) {
            val f = if (animate) VignetteMotion.of(vignette, progress.value) else held
            val cx = size.width / 2f
            val cy = size.height * 0.46f
            val radius = size.minDimension * 0.16f

            // Ripple rings where the friend lands (leap and cannonball).
            if (f.ring > 0f) {
                repeat(2) { index ->
                    val phase = (f.ring - index * 0.18f).coerceIn(0f, 1f)
                    if (phase > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.34f * (1f - phase)),
                            radius = radius * (0.7f + 1.5f * phase),
                            center = Offset(cx, cy + radius * 1.15f),
                            style = Stroke(width = size.minDimension * 0.006f),
                        )
                    }
                }
            }
            // The bubble the friend floats up inside.
            if (f.bubble > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.22f * f.bubble), Color.Transparent),
                        center = Offset(cx - radius * 0.3f, cy - radius * 0.3f),
                        radius = radius * 1.8f,
                    ),
                    radius = radius * 1.35f,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * f.bubble),
                    radius = radius * 1.35f,
                    center = Offset(cx, cy),
                    style = Stroke(width = size.minDimension * 0.004f),
                )
            }
            // The mark: a droplet, under the friend.
            if (f.logo) {
                val markAlpha = 1f - exit
                val drop = Path().apply {
                    moveTo(cx, cy - radius * 1.5f)
                    cubicTo(
                        cx + radius * 0.9f, cy - radius * 0.4f,
                        cx + radius * 0.62f, cy + radius * 0.35f,
                        cx, cy + radius * 0.35f,
                    )
                    cubicTo(
                        cx - radius * 0.62f, cy + radius * 0.35f,
                        cx - radius * 0.9f, cy - radius * 0.4f,
                        cx, cy - radius * 1.5f,
                    )
                    close()
                }
                drawPath(drop, Color(0xFF5FD3F3).copy(alpha = 0.85f * markAlpha))
                drawPath(
                    drop,
                    Color.White.copy(alpha = 0.55f * markAlpha),
                    style = Stroke(width = size.minDimension * 0.004f),
                )
            }
        }

        // The friend. Every value is read inside the layer lambda, so the flight
        // invalidates a layer and not the screen.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val stage = if (animate) VignetteMotion.of(vignette, progress.value) else held
                    translationX = stage.offsetX * size.width
                    translationY = stage.offsetY * size.height
                    scaleX = stage.scale
                    scaleY = stage.scale * stage.squash
                    rotationZ = stage.rotation
                    alpha = 1f - exit
                },
            contentAlignment = Alignment.Center,
        ) {
            FriendAvatar(
                friend = friend,
                mood = Mood.HAPPY,
                size = 132.dp,
                animate = false,
                hat = hat,
                outfit = outfit,
            )
        }

        // The wordmark, outside the friend's layer so it never spins.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 170.dp)
                .graphicsLayer { alpha = (1f - exit) * 0.9f },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sippy",
                style = SippyType.title,
                color = Color.White,
            )
            Text(
                text = "drink up",
                style = SippyType.caption,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
