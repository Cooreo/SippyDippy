package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.nav.LensBounds
import com.cooreo.sippy.domain.nav.LensGeometry
import com.cooreo.sippy.domain.nav.NavLens
import com.cooreo.sippy.ui.glass.GlassLevel
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.CappedTextScale
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import kotlin.math.abs
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
 * The floating glass pill from the site nav (round 6, rebuilt for round 8 B6,
 * bound to measured tab rectangles via [LensGeometry] for round 9 2.1).
 *
 * One liquid lens travels the bar behind the glyphs: it springs between the
 * measured tab bounds, stretches along its travel in proportion to its speed,
 * stays inside the bar's insets, and magnifies whichever glyph it is under.
 * Dragging moves the lens under the finger and snaps it to the nearest tab on
 * release. The Log button sits on top of the lens, so the lens slides behind it
 * and the button stays tappable the whole way.
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
    /** Where the + button ended up, so the Log sheet can grow out of it (B7). */
    onLogBounds: (Rect) -> Unit = {},
) {
    val c = LocalSippyColors.current
    val density = LocalDensity.current.density
    val barShape = RoundedCornerShape(Dimens.pill)
    val lensShape = RoundedCornerShape(Dimens.pill)
    val scope = rememberCoroutineScope()

    var barBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val tabRootBounds = remember { mutableStateMapOf<NavTarget, Rect>() }

    var initialized by remember { mutableStateOf(false) }
    var fromTarget by remember { mutableStateOf(current) }
    var toTarget by remember { mutableStateOf(current) }
    var fromBounds by remember { mutableStateOf<LensBounds?>(null) }
    var dragSnapBounds by remember { mutableStateOf<LensBounds?>(null) }
    var dragCenterX by remember { mutableStateOf<Float?>(null) }
    var segmentSlots by remember { mutableFloatStateOf(1f) }
    val travel = remember { Animatable(1f) }
    val reveal = remember { Animatable(1f) }

    fun measuredBounds(target: NavTarget): LensBounds? {
        val bar = barBoundsInRoot
        val rect = tabRootBounds[target]
        if (bar == Rect.Zero || rect == null || rect == Rect.Zero) return null
        return LensBounds(
            left = rect.left - bar.left,
            top = rect.top - bar.top,
            width = rect.width,
            height = rect.height,
        )
    }

    fun fallbackBounds(target: NavTarget, barWidthPx: Float, barHeightPx: Float): LensBounds {
        val inset = LensGeometry.INSET_DP * density
        val innerW = (barWidthPx - inset * 2f).coerceAtLeast(1f)
        val slotW = innerW / NavLens.SLOTS.toFloat()
        val slot = slotOf(target)
        return LensBounds(
            left = inset + slot * slotW,
            top = inset,
            width = slotW,
            height = (barHeightPx - inset * 2f).coerceAtLeast(1f),
        )
    }

    fun boundsFor(target: NavTarget, barWidthPx: Float, barHeightPx: Float): LensBounds =
        measuredBounds(target) ?: fallbackBounds(target, barWidthPx, barHeightPx)

    fun resolveSegment(barWidthPx: Float, barHeightPx: Float): Triple<LensBounds, LensBounds, Float> {
        val dragged = dragCenterX
        if (dragged != null) {
            val ordered = TAB_TARGETS.map { boundsFor(it, barWidthPx, barHeightPx) }
            val clampedX = dragged.coerceIn(ordered.first().centerX, ordered.last().centerX)
            for (i in 0 until ordered.lastIndex) {
                val a = ordered[i]
                val b = ordered[i + 1]
                if (clampedX <= b.centerX || i == ordered.lastIndex - 1) {
                    val span = b.centerX - a.centerX
                    val t = if (span > 0f) ((clampedX - a.centerX) / span).coerceIn(0f, 1f) else 0f
                    return Triple(a, b, t)
                }
            }
        }
        val from = fromBounds ?: boundsFor(fromTarget, barWidthPx, barHeightPx)
        val to = boundsFor(toTarget, barWidthPx, barHeightPx)
        return Triple(from, to, travel.value.coerceIn(0f, 1f))
    }

    fun currentCenterBounds(): LensBounds? {
        if (barBoundsInRoot == Rect.Zero) return null
        val (from, to, t) = resolveSegment(barBoundsInRoot.width, barBoundsInRoot.height)
        val w = from.width + (to.width - from.width) * t
        val cx = from.centerX + (to.centerX - from.centerX) * t
        return LensBounds(
            left = cx - w / 2f,
            top = from.top,
            width = w,
            height = from.height,
        )
    }

    fun lensCenterX(): Float {
        if (barBoundsInRoot == Rect.Zero) return 0f
        val (from, to, t) = resolveSegment(barBoundsInRoot.width, barBoundsInRoot.height)
        return from.centerX + (to.centerX - from.centerX) * t
    }

    LaunchedEffect(current, animate) {
        if (!initialized) {
            initialized = true
            fromTarget = current
            toTarget = current
            fromBounds = null
            travel.snapTo(1f)
            reveal.snapTo(1f)
            return@LaunchedEffect
        }
        val snapped = dragSnapBounds
        dragSnapBounds = null
        val previous = toTarget
        fromBounds = snapped ?: currentCenterBounds()
        fromTarget = previous
        toTarget = current
        segmentSlots = abs(slotOf(current) - slotOf(previous)).coerceAtLeast(1).toFloat()
        dragCenterX = null
        if (animate) {
            reveal.snapTo(1f)
            travel.snapTo(0f)
            travel.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f))
            fromBounds = null
        } else {
            // Reduce effects: the lens jumps, and a short fade hides the cut.
            travel.snapTo(1f)
            fromBounds = null
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
            shape = barShape,
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.LIVE,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { barBoundsInRoot = it.boundsInRoot() },
            ) {
                // The lens, sized and positioned directly from measured tab bounds
                // via LensGeometry. No fixed 56x52dp circle, no second scaleX stretch.
                Box(
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val barW = barBoundsInRoot.width.coerceAtLeast(1f)
                            val barH = barBoundsInRoot.height.coerceAtLeast(1f)
                            val (from, to, t) = resolveSegment(barW, barH)
                            val speedSlots = if (dragCenterX != null) {
                                0f
                            } else {
                                travel.velocity * segmentSlots
                            }
                            val raw = LensGeometry.lensRect(
                                from = from,
                                to = to,
                                progress = t,
                                velocity = speedSlots,
                                barHeightPx = barH,
                                density = density,
                            )
                            val clamped = LensGeometry.clampToBar(
                                rect = raw,
                                barWidthPx = barW,
                                barHeightPx = barH,
                                density = density,
                            )
                            val placeable = measurable.measure(
                                Constraints.fixed(
                                    width = clamped.width.roundToInt().coerceAtLeast(1),
                                    height = clamped.height.roundToInt().coerceAtLeast(1),
                                ),
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(
                                    x = clamped.left.roundToInt(),
                                    y = clamped.top.roundToInt(),
                                )
                            }
                        }
                        .graphicsLayer {
                            alpha = if (barBoundsInRoot == Rect.Zero) {
                                0f
                            } else {
                                reveal.value.coerceIn(0f, 1f)
                            }
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    c.accentBright.copy(alpha = 0.46f),
                                    c.accent.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = lensShape,
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.36f), Color.Transparent),
                            ),
                            shape = lensShape,
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LensGeometry.INSET_DP.dp,
                            vertical = LensGeometry.INSET_DP.dp,
                        )
                        .height(56.dp)
                        .pointerInput(animate) {
                            if (!animate) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    if (barBoundsInRoot == Rect.Zero) return@detectHorizontalDragGestures
                                    dragCenterX = lensCenterX()
                                },
                                onDragEnd = {
                                    val cx = dragCenterX ?: return@detectHorizontalDragGestures
                                    val barW = barBoundsInRoot.width
                                    val barH = barBoundsInRoot.height
                                    if (barW <= 0f || barH <= 0f) {
                                        dragCenterX = null
                                        return@detectHorizontalDragGestures
                                    }
                                    val snap = currentCenterBounds()
                                    val nearest = TAB_TARGETS.minByOrNull { target ->
                                        abs(boundsFor(target, barW, barH).centerX - cx)
                                    } ?: current
                                    dragCenterX = null
                                    if (nearest != current) {
                                        dragSnapBounds = snap
                                        onSelect(nearest)
                                    } else {
                                        fromBounds = snap
                                        fromTarget = current
                                        toTarget = current
                                        segmentSlots = 1f
                                        scope.launch {
                                            travel.snapTo(0f)
                                            travel.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.62f,
                                                    stiffness = 380f,
                                                ),
                                            )
                                            fromBounds = null
                                        }
                                    }
                                },
                                onDragCancel = {
                                    val snap = currentCenterBounds()
                                    dragCenterX = null
                                    fromBounds = snap
                                    fromTarget = current
                                    toTarget = current
                                    segmentSlots = 1f
                                    scope.launch {
                                        travel.snapTo(0f)
                                        travel.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
                                        )
                                        fromBounds = null
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (barBoundsInRoot == Rect.Zero) return@detectHorizontalDragGestures
                                    val barW = barBoundsInRoot.width
                                    val barH = barBoundsInRoot.height
                                    val ordered = TAB_TARGETS.map { boundsFor(it, barW, barH) }
                                    val base = dragCenterX ?: lensCenterX()
                                    dragCenterX = (base + dragAmount)
                                        .coerceIn(ordered.first().centerX, ordered.last().centerX)
                                },
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(
                        target = NavTarget.HOME,
                        current = current,
                        onSelect = onSelect,
                        onBounds = { tabRootBounds[NavTarget.HOME] = it },
                        proximity = {
                            val b = measuredBounds(NavTarget.HOME)
                            if (b != null && b.width > 0f && barBoundsInRoot != Rect.Zero) {
                                (1f - abs(lensCenterX() - b.centerX) / b.width).coerceIn(0f, 1f)
                            } else if (current == NavTarget.HOME) {
                                1f
                            } else {
                                0f
                            }
                        },
                    )
                    NavItem(
                        target = NavTarget.DRINKS,
                        current = current,
                        onSelect = onSelect,
                        onBounds = { tabRootBounds[NavTarget.DRINKS] = it },
                        proximity = {
                            val b = measuredBounds(NavTarget.DRINKS)
                            if (b != null && b.width > 0f && barBoundsInRoot != Rect.Zero) {
                                (1f - abs(lensCenterX() - b.centerX) / b.width).coerceIn(0f, 1f)
                            } else if (current == NavTarget.DRINKS) {
                                1f
                            } else {
                                0f
                            }
                        },
                    )
                    Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                        LogButton(onLog, onLogBounds)
                    }
                    NavItem(
                        target = NavTarget.FRIENDS,
                        current = current,
                        onSelect = onSelect,
                        onBounds = { tabRootBounds[NavTarget.FRIENDS] = it },
                        proximity = {
                            val b = measuredBounds(NavTarget.FRIENDS)
                            if (b != null && b.width > 0f && barBoundsInRoot != Rect.Zero) {
                                (1f - abs(lensCenterX() - b.centerX) / b.width).coerceIn(0f, 1f)
                            } else if (current == NavTarget.FRIENDS) {
                                1f
                            } else {
                                0f
                            }
                        },
                    )
                    NavItem(
                        target = NavTarget.SETTINGS,
                        current = current,
                        onSelect = onSelect,
                        onBounds = { tabRootBounds[NavTarget.SETTINGS] = it },
                        proximity = {
                            val b = measuredBounds(NavTarget.SETTINGS)
                            if (b != null && b.width > 0f && barBoundsInRoot != Rect.Zero) {
                                (1f - abs(lensCenterX() - b.centerX) / b.width).coerceIn(0f, 1f)
                            } else if (current == NavTarget.SETTINGS) {
                                1f
                            } else {
                                0f
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    target: NavTarget,
    current: NavTarget,
    onSelect: (NavTarget) -> Unit,
    onBounds: (Rect) -> Unit,
    proximity: () -> Float,
) {
    val c = LocalSippyColors.current
    val isCurrent = current == target
    val tint = if (isCurrent) c.accentBright else c.text2

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .clip(RoundedCornerShape(Dimens.pill))
            .clickable(role = Role.Tab) { onSelect(target) }
            .semantics {
                contentDescription = target.label
                selected = isCurrent
            }
            .graphicsLayer {
                // Read in the layer scope: the lens travelling does not recompose.
                val near = proximity()
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
        // Decorative: capped at 1.3x so a large system font cannot push the label
        // out of its slot (round 9, 1.2). Tab label floor is 11sp (SippyType.tab).
        CappedTextScale {
            Text(
                text = target.label,
                color = tint,
                style = SippyType.tab,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    // Faint at rest, full under the lens: the label is magnified with
                    // the glyph instead of popping in and out.
                    alpha = 0.45f + 0.55f * proximity()
                },
            )
        }
    }
}

@Composable
private fun LogButton(onLog: () -> Unit, onBounds: (Rect) -> Unit) {
    val c = LocalSippyColors.current
    // B7: a press squashes the button and sends one ripple out through it. Drawn,
    // not the platform's indication: the rest of the app silences indications on
    // purpose, and a drawn ripple cannot be clipped differently per API level.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.30f, stiffness = 780f),
        label = "logSquash",
    )
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(420))
        }
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .graphicsLayer {
                scaleX = 1f + (1f - squash) * 0.85f
                scaleY = squash
            }
            .clip(CircleShape)
            .background(c.accent)
            .drawBehind {
                // Clipped by the circle above, so it can never bleed out of the
                // button as it grows.
                val p = ripple.value.coerceIn(0f, 1f)
                if (p > 0f && p < 1f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f * (1f - p)),
                        radius = size.minDimension * 0.62f * p,
                        center = center,
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onLog,
            )
            .semantics { contentDescription = "Log a drink" },
        contentAlignment = Alignment.Center,
    ) {
        NavGlyph(kind = NavGlyphKind.LOG, tint = Color.White, size = 26.dp, strokeWidth = 2.3.dp)
    }
}
