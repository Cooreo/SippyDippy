package com.cooreo.sippy.ui.glass

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.system.LocalMotionAllowed
import com.cooreo.sippy.ui.system.motionAllowed
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Glass capability tier (brief §glass):
 *  - LENS  — API 33+: real refraction (`lens`) on top of a blur;
 *  - BLUR  — API 31..32: backdrop blur, no refraction;
 *  - FROST — API < 31, or "reduce effects": static frosting, no GPU work.
 * The composition itself never changes between tiers, only the effects.
 */
enum class GlassTier { LENS, BLUR, FROST;
    companion object {
        fun forSdk(sdkInt: Int = Build.VERSION.SDK_INT, reduceEffects: Boolean = false): GlassTier = when {
            reduceEffects -> FROST
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> LENS
            sdkInt >= Build.VERSION_CODES.S -> BLUR
            else -> FROST
        }
    }
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalGlassTier = staticCompositionLocalOf { GlassTier.FROST }

/**
 * The one fixed backdrop the whole app is drawn on (SPEC §1: a single
 * full-viewport layer; content is transparent). It is captured once into a
 * layer, and every glass surface samples it — no per-surface backdrops.
 */
@Composable
fun SippyBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxSize()) {
        AmbientBackground(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        )
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * The site's underwater scene, static: the deep gradient, three light pools, a
 * diagonal shaft, drifting caustic ribbons and ambient droplets. Nothing here
 * animates and no image is loaded — but the detail is deliberate (round 7, A2):
 * a blur or a lens over a smooth gradient is invisible, so the backdrop has to
 * carry structure if the glass is to look like glass.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val c = LocalSippyColors.current
    val dark = c.isDark
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(c.bg0, c.bg1, c.bg2)),
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun pool(x: Float, y: Float, r: Float, color: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent),
                        center = Offset(size.width * x, size.height * y),
                        radius = size.minDimension * r,
                    ),
                    radius = size.minDimension * r,
                    center = Offset(size.width * x, size.height * y),
                )
            }
            val accent = c.accent.copy(alpha = if (dark) 0.34f else 0.26f)
            pool(0.16f, 0.12f, 0.55f, accent)
            pool(0.92f, 0.34f, 0.42f, c.accentBright.copy(alpha = if (dark) 0.20f else 0.18f))
            pool(0.44f, 0.98f, 0.60f, c.accentDeep.copy(alpha = if (dark) 0.16f else 0.10f))

            // Light shaft from the top-left, like the hero scrim on the site.
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.06f, 0f)
                    lineTo(size.width * 0.42f, 0f)
                    lineTo(size.width * 0.30f, size.height)
                    lineTo(size.width * -0.04f, size.height)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (dark) 0.05f else 0.16f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.9f,
                ),
            )

            // Caustic ribbons: three travelling wave bands, painted as strokes so
            // their edges are what the lens bends.
            val caustic = if (dark) 0.10f else 0.20f
            repeat(3) { band ->
                val baseY = size.height * (0.30f + band * 0.22f)
                val amplitude = size.height * (0.045f + band * 0.012f)
                val path = Path()
                var x = -size.width * 0.05f
                var first = true
                while (x <= size.width * 1.05f) {
                    val y = baseY + sin(x / size.width * 5.2f + band * 1.7f) * amplitude
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    x += size.width / 40f
                }
                drawPath(
                    path = path,
                    color = c.accentBright.copy(alpha = caustic * (1f - band * 0.22f)),
                    style = Stroke(width = (2.5f - band * 0.5f).dp.toPx()),
                )
            }

            // Ambient droplets: fixed positions, small, soft.
            val droplets = floatArrayOf(
                0.08f, 0.16f, 0.030f,
                0.24f, 0.34f, 0.018f,
                0.38f, 0.12f, 0.022f,
                0.52f, 0.44f, 0.016f,
                0.66f, 0.22f, 0.026f,
                0.74f, 0.62f, 0.020f,
                0.86f, 0.40f, 0.030f,
                0.92f, 0.74f, 0.017f,
                0.16f, 0.68f, 0.021f,
                0.46f, 0.80f, 0.026f,
                0.60f, 0.92f, 0.015f,
                0.30f, 0.52f, 0.013f,
            )
            var i = 0
            while (i + 2 < droplets.size) {
                val x = droplets[i] * size.width
                val y = droplets[i + 1] * size.height
                val r = droplets[i + 2] * size.minDimension
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (dark) 0.16f else 0.30f),
                            Color.Transparent,
                        ),
                        center = Offset(x - r * 0.3f, y - r * 0.3f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(x, y),
                )
                i += 3
            }
        }
    }
}

/**
 * A glass surface: backdrop effects behind, one tinted surface, a rim
 * highlight and a soft drop shadow. [content] sits on the single surface — no
 * nested backdrop-filter layers (SPEC §1 budget).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.card),
    tint: Color? = null,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalSippyColors.current
    val backdrop = LocalBackdrop.current
    val tier = LocalGlassTier.current
    val surface = tint ?: c.glassTint

    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                when (tier) {
                    GlassTier.LENS -> {
                        blur(radius = 14.dp.toPx())
                        lens(refractionHeight = 26.dp.toPx(), refractionAmount = 22.dp.toPx())
                    }
                    GlassTier.BLUR -> blur(radius = 18.dp.toPx())
                    // Static frosting: tint + rim only, zero GPU work.
                    GlassTier.FROST -> Unit
                }
            },
            highlight = { Highlight.Default },
            shadow = if (shadow) {
                { Shadow(radius = 22.dp, color = Color.Black.copy(alpha = if (c.isDark) 0.42f else 0.16f)) }
            } else {
                null
            },
            onDrawSurface = { drawRect(surface) },
        )
    } else {
        Modifier.background(surface, shape)
    }
    // A real rim: bright at the top, fading at the bottom, drawn on top of the
    // surface. It is what makes a card read as glass even at FROST (A2).
    val rim = Modifier.border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(c.glassRimStart, c.glassRimEnd)),
        shape = shape,
    )
    Box(modifier = modifier.clip(shape).then(glass).then(rim), content = content)
}

/**
 * Press feedback for a glass control: 0.97 scale, transform only. Instant when
 * motion is off (Reduce effects or the system animator scale), so a control is
 * never mid-animation for a player who asked for stillness.
 */
@Composable
private fun pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val moving = motionAllowed() && LocalMotionAllowed.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && moving) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "pressScale",
    )
    return Modifier.graphicsLayer {
        scaleX = if (moving) scale else 1f
        scaleY = if (moving) scale else 1f
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.card),
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(modifier = modifier, shape = shape, tint = tint) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * A tappable glass pill. Always at least 48dp tall (a11y), with a [Role] and a
 * content description supplied by the caller through [modifier] semantics.
 */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    val tint = when {
        !enabled -> c.glassTint.copy(alpha = 0.4f)
        prominent -> c.accent.copy(alpha = 0.82f)
        else -> null
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touch, minWidth = Dimens.touch)
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                        .then(pressScale(interaction))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            tint = tint,
            shadow = false,
        ) {
            Box(Modifier.padding(contentPadding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (prominent) Color.White else c.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Small glass chip used for accent swatches, drink shortcuts and tags. */
@Composable
fun GlassChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touch, minWidth = Dimens.touch)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(pressScale(interaction)),
        contentAlignment = Alignment.Center,
    ) {
        // matchParentSize: the visible pill is exactly the cell the caller
        // measured, so chips in a weighted row line up (A4).
        GlassSurface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            tint = if (selected) c.accent.copy(alpha = 0.72f) else null,
            shadow = false,
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (selected) Color.White else c.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
