package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.nav.NavLens
import com.cooreo.sippy.ui.glass.GlassLevel
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The height the floating pill occupies inside the content frame:
 * 56 dp item + 2x8 dp row padding + 2x10 dp margins. Screens reserve this (plus
 * the navigation inset) as bottom content padding.
 */
val NavBarHeight = 92.dp

/** The four destinations behind the bottom bar's four glyphs. */
enum class NavTarget(val label: String) {
    HOME("Home"),
    DRINKS("Drinks"),
    FRIENDS("Friends"),
    SETTINGS("Settings"),
}

/** Left to right; the Log button owns the middle slot and no tab may sit on it. */
private val TAB_TARGETS = listOf(
    NavTarget.HOME,
    NavTarget.DRINKS,
    NavTarget.FRIENDS,
    NavTarget.SETTINGS,
)

private fun slotOf(target: NavTarget): Int =
    NavLens.TAB_SLOTS[TAB_TARGETS.indexOf(target).coerceAtLeast(0)]

/**
 * The floating glass pill from the site nav (round 6, rebuilt for round 8 B6).
 *
 * One liquid lens travels the bar behind the glyphs: it springs to the active tab,
 * stretches along its travel in proportion to its speed, settles with a slight
 * overshoot, and magnifies whichever glyph it is under. Dragging moves the lens
 * under the finger and snaps it to the nearest tab on release. The Log button
 * sits on top of the lens, so the lens slides behind it and the button stays
 * tappable the whole way.
 *
 * With Reduce effects the lens does not travel: it fades in place instead.
 */
@Composable
fun FloatingNav(
    current: NavTarget,
    onSelect: (NavTarget) -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    val lens = remember { Animatable(slotOf(current).toFloat()) }
    val reveal = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(current, animate) {
        val target = slotOf(current).toFloat()
        if (animate) {
            reveal.snapTo(1f)
            lens.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 380f))
        } else {
            // Reduce effects: the lens jumps, and a short fade hides the cut.
            lens.snapTo(target)
            reveal.snapTo(0.35f)
            reveal.animateTo(1f, tween(150))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.screenPadding, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassSurface(
            shape = shape,
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.LIVE,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                val slotPx = constraints.maxWidth / NavLens.SLOTS.toFloat()

                // The lens, drawn first so the row and the Log button sit on top.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val center = NavLens.centerPx(lens.value, slotPx)
                            IntOffset((center - slotPx / 2f).roundToInt(), 0)
                        }
                        .size(width = 56.dp, height = 52.dp)
                        .graphicsLayer {
                            val speed = lens.velocity
                            scaleX = NavLens.stretchX(speed)
                            scaleY = NavLens.stretchY(speed)
                            alpha = reveal.value.coerceIn(0f, 1f)
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    c.accentBright.copy(alpha = 0.46f),
                                    c.accent.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.36f), Color.Transparent),
                            ),
                            shape = CircleShape,
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pointerInput(animate, slotPx) {
                            if (!animate) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val slot = NavLens.nearestTabSlot(lens.value)
                                    val target = TAB_TARGETS
                                        .getOrNull(NavLens.TAB_SLOTS.indexOf(slot))
                                    target?.let { onSelect(it) }
                                    scope.launch {
                                        lens.animateTo(
                                            targetValue = slot.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = 0.62f,
                                                stiffness = 380f,
                                            ),
                                        )
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        lens.animateTo(
                                            targetValue = slotOf(current).toFloat(),
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
                                        )
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val next = NavLens.clamp(lens.value + dragAmount / slotPx)
                                    scope.launch { lens.snapTo(next) }
                                },
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(NavTarget.HOME, current, lens, onSelect)
                    NavItem(NavTarget.DRINKS, current, lens, onSelect)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        LogButton(onLog)
                    }
                    NavItem(NavTarget.FRIENDS, current, lens, onSelect)
                    NavItem(NavTarget.SETTINGS, current, lens, onSelect)
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    target: NavTarget,
    current: NavTarget,
    lens: Animatable<Float, AnimationVector1D>,
    onSelect: (NavTarget) -> Unit,
) {
    val c = LocalSippyColors.current
    val isCurrent = current == target
    val tint = if (isCurrent) c.accentBright else c.text2
    val slot = slotOf(target)

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(Dimens.pill))
            .clickable(role = Role.Tab) { onSelect(target) }
            .semantics {
                contentDescription = target.label
                selected = isCurrent
            }
            .graphicsLayer {
                // Read in the layer scope: the lens travelling does not recompose.
                val near = NavLens.proximity(lens.value, slot)
                val scale = 1f + 0.18f * near
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavGlyph(
            kind = when (target) {
                NavTarget.HOME -> NavGlyphKind.HOME
                NavTarget.DRINKS -> NavGlyphKind.DRINKS
                NavTarget.FRIENDS -> NavGlyphKind.FRIENDS
                NavTarget.SETTINGS -> NavGlyphKind.SETTINGS
            },
            tint = tint,
            size = 22.dp,
        )
        Text(
            text = target.label,
            color = tint,
            fontSize = 9.sp,
            modifier = Modifier.graphicsLayer {
                // Faint at rest, full under the lens: the label is magnified with
                // the glyph instead of popping in and out.
                alpha = 0.45f + 0.55f * NavLens.proximity(lens.value, slot)
            },
        )
    }
}

@Composable
private fun LogButton(onLog: () -> Unit) {
    val c = LocalSippyColors.current
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(c.accent)
            .clickable(role = Role.Button, onClick = onLog)
            .semantics { contentDescription = "Log a drink" },
        contentAlignment = Alignment.Center,
    ) {
        NavGlyph(kind = NavGlyphKind.LOG, tint = Color.White, size = 26.dp, strokeWidth = 2.3.dp)
    }
}
