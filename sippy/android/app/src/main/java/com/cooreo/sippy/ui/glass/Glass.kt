package com.cooreo.sippy.ui.glass

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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

/** Static ambient gradient + three soft light pools. No animation, no images. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val c = LocalSippyColors.current
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
            val accent = c.accent.copy(alpha = if (c.isDark) 0.34f else 0.26f)
            pool(0.16f, 0.12f, 0.55f, accent)
            pool(0.92f, 0.34f, 0.42f, c.accentBright.copy(alpha = if (c.isDark) 0.20f else 0.18f))
            pool(0.44f, 0.98f, 0.60f, c.accentDeep.copy(alpha = if (c.isDark) 0.16f else 0.10f))
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
    Box(modifier = modifier.clip(shape).then(glass), content = content)
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
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touch, minWidth = Dimens.touch)
            .clip(shape)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = shape, tint = tint, shadow = false) {
            Box(Modifier.padding(contentPadding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (prominent) Color.White else c.text1,
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
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touch, minWidth = Dimens.touch)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            shape = shape,
            tint = if (selected) c.accent.copy(alpha = 0.72f) else null,
            shadow = false,
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (selected) Color.White else c.text1,
                )
            }
        }
    }
}
