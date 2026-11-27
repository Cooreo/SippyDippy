package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors

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

/**
 * The floating glass pill from the site nav: four glyphs around a Sippy Blue
 * Log action. It floats over the content, honours the gesture nav inset, and
 * every hit area is at least 48dp. The whole bar is one glass surface, so it
 * costs one backdrop layer.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.screenPadding, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassSurface(shape = shape, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem(NavTarget.HOME, current, onSelect, animate)
                NavItem(NavTarget.DRINKS, current, onSelect, animate)
                LogButton(onLog)
                NavItem(NavTarget.FRIENDS, current, onSelect, animate)
                NavItem(NavTarget.SETTINGS, current, onSelect, animate)
            }
        }
    }
}

@Composable
private fun NavItem(
    target: NavTarget,
    current: NavTarget,
    onSelect: (NavTarget) -> Unit,
    animate: Boolean,
) {
    val c = LocalSippyColors.current
    val isCurrent = current == target
    val tint = if (isCurrent) c.accentBright else c.text2
    // The liquid lens: it swells under the active glyph with a spring overshoot
    // (and squashes on the way, so it reads as a droplet settling), and shrinks
    // away when the tab is left.
    val lens by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = if (animate) spring(dampingRatio = 0.5f, stiffness = 420f) else snap(),
        label = "navLens",
    )
    val lensAmount = if (animate) lens else if (isCurrent) 1f else 0f

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(Dimens.pill))
            .clickable(role = Role.Tab) { onSelect(target) }
            .semantics {
                contentDescription = target.label
                selected = isCurrent
            },
        contentAlignment = Alignment.Center,
    ) {
        if (lensAmount > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .graphicsLayer {
                        scaleX = 0.84f + 0.16f * lensAmount
                        scaleY = 0.68f + 0.32f * lensAmount
                        alpha = lensAmount.coerceIn(0f, 1f)
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c.accentBright.copy(alpha = 0.42f),
                                c.accent.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.34f * lensAmount), Color.Transparent),
                        ),
                        shape = CircleShape,
                    ),
            )
        }
        NavGlyph(
            kind = when (target) {
                NavTarget.HOME -> NavGlyphKind.HOME
                NavTarget.DRINKS -> NavGlyphKind.DRINKS
                NavTarget.FRIENDS -> NavGlyphKind.FRIENDS
                NavTarget.SETTINGS -> NavGlyphKind.SETTINGS
            },
            tint = tint,
            size = 23.dp,
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
