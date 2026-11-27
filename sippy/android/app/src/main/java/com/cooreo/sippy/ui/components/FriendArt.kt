package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.cooreo.sippy.R
import com.cooreo.sippy.model.FriendKind

/** Moods from the brief. Growth is shown by scale only (no new art). */
enum class Mood { HAPPY, DANCING, SLEEPY, SAD }

@Composable
fun friendPainter(kind: FriendKind, mood: Mood) = painterResource(
    when (kind) {
        FriendKind.DIPPY -> when (mood) {
            Mood.HAPPY -> R.drawable.dippy_happy
            Mood.DANCING -> R.drawable.dippy_dancing
            Mood.SLEEPY -> R.drawable.dippy_sleepy
            // No sad art exists yet (brief: no new art this round) — the sleepy
            // droplet, desaturated, reads as "let down" without new assets.
            Mood.SAD -> R.drawable.dippy_sleepy
        },
        FriendKind.PLANT -> when (mood) {
            Mood.HAPPY, Mood.DANCING -> R.drawable.friend_plant_bloom
            Mood.SLEEPY, Mood.SAD -> R.drawable.friend_plant_sprout
        },
        FriendKind.CAT -> when (mood) {
            Mood.HAPPY, Mood.DANCING -> R.drawable.friend_pet_adult
            Mood.SLEEPY, Mood.SAD -> R.drawable.friend_pet_cub
        },
    },
)

@Composable
fun FriendImage(
    kind: FriendKind,
    mood: Mood,
    growthStreak: Int,
    size: Int,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val target = growthScale(growthStreak)
    val animated by animateFloatAsState(
        targetValue = if (animate) target else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label = "friendGrowth",
    )
    val sadFilter = if (mood == Mood.SAD) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
    } else {
        null
    }
    Image(
        painter = friendPainter(kind, mood),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = sadFilter,
        modifier = modifier.scale(if (animate) animated else 1f),
    )
}

/** Streak milestone → scale. Scale only, per the brief. */
fun growthScale(grownAtStreak: Int): Float = when {
    grownAtStreak >= 100 -> 1.45f
    grownAtStreak >= 30 -> 1.30f
    grownAtStreak >= 14 -> 1.20f
    grownAtStreak >= 7 -> 1.10f
    grownAtStreak >= 3 -> 1.04f
    else -> 1f
}
