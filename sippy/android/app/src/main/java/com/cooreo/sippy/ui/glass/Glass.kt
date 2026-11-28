package com.cooreo.sippy.ui.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.system.LocalMotionAllowed
import com.cooreo.sippy.ui.system.motionAllowed
import com.cooreo.sippy.ui.theme.BackdropSamples
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.sin

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
 * Developer-only exaggeration factor for the glass (round 8, B1). 1f is the
 * shipping look; 2.4f makes the refraction and the blur impossible to miss, so
 * "is the lens actually running?" is a switch, not an argument.
 */
val LocalGlassBoost = staticCompositionLocalOf { 1f }

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
    // B2: the host owns the tab tint; a screen writes LocalBackdropTint.color and
    // the backdrop crossfades to it.
    val tint = remember { BackdropTint() }
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBackdropTint provides tint) {
            AmbientBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            )
        }
        // 1.1: the theme scrim. The backdrop's art and its animation are untouched;
        // this is a navy veil over them, light at the top so the light shafts still
        // read, heavy at the bottom so text always has something dark to sit on.
        ThemeScrim()
        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalBackdropTint provides tint,
        ) {
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

/**
 * The dark-theme veil over the backdrop (round 9, 1.1): 35% navy at the top, 75% at
 * the bottom. Light theme has no scrim — its backdrop is the pale one.
 */
@Composable
private fun ThemeScrim() {
    val c = LocalSippyColors.current
    if (!c.isDark) return
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0B1F38).copy(alpha = BackdropSamples.SCRIM_TOP_ALPHA),
                    1f to Color(0xFF0B1F38).copy(alpha = BackdropSamples.SCRIM_BOTTOM_ALPHA),
                ),
            ),
    )
}

/**
 * A glass surface: backdrop effects behind, one tinted surface, a rim
 * highlight and a soft drop shadow. [content] sits on the single surface — no
 * nested backdrop-filter layers (SPEC §1 budget).
 */
/**
 * The three glass levels (round 8, B3). This *is* the performance budget:
 *
 *  - LIVE — the real thing: backdrop blur plus the AGSL lens. The orb overlay,
 *    the bottom bar, the + button and sheets. Never more than three at once.
 *  - CHEAP — level 2: tint, gradient rim, inner top highlight, soft blue shadow,
 *    no live blur. Every list card and the Today card.
 *  - CHIP — level 3: a thin tint and a rim. Chips, pills, tags.
 *
 * Reduce effects (or a device without RenderEffect) collapses everything to
 * CHEAP, so the app never blurs on a phone that asked it not to.
 */
enum class GlassLevel { LIVE, CHEAP, CHIP }

val LocalGlassLevel = staticCompositionLocalOf { GlassLevel.CHEAP }

/**
 * A glass surface. [level] overrides [LocalGlassLevel] for one call; null means
 * "use the ambient level", which is what most call sites want.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.card),
    tint: Color? = null,
    shadow: Boolean = true,
    level: GlassLevel? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalSippyColors.current
    val backdrop = LocalBackdrop.current
    val tier = LocalGlassTier.current
    val boost = LocalGlassBoost.current
    val requested = level ?: LocalGlassLevel.current
    val effective = if (tier == GlassTier.FROST) GlassLevel.CHEAP else requested
    val surface = if (effective == GlassLevel.CHIP) {
        // 0.68 rather than 0.55: at 0.55 a chip over the brightest part of the
        // backdrop is too pale for its own secondary text (see ContrastTest).
        (tint ?: c.glassTint).copy(alpha = 0.68f)
    } else {
        tint ?: c.glassTint
    }

    val glass = when {
        // Level 1 — the lens. Only reachable on API 33+ with shaders.
        effective == GlassLevel.LIVE && backdrop != null -> Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(radius = 14.dp.toPx() * boost)
                lens(
                    refractionHeight = 26.dp.toPx() * boost,
                    refractionAmount = 22.dp.toPx() * boost,
                )
            },
            highlight = { Highlight.Default },
            shadow = if (shadow) {
                { Shadow(radius = 22.dp, color = Color.Black.copy(alpha = if (c.isDark) 0.42f else 0.16f)) }
            } else {
                null
            },
            onDrawSurface = { drawRect(surface) },
        )

        // Level 2 — cheap glass: a tinted plate with a soft blue shadow. No blur,
        // so it costs nothing per frame while scrolling.
        effective == GlassLevel.CHEAP -> Modifier
            .then(
                if (shadow) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color(0xFF2E7CF6).copy(alpha = 0.30f),
                        spotColor = Color(0xFF2E7CF6).copy(alpha = 0.26f),
                    )
                } else {
                    Modifier
                },
            )
            .background(surface, shape)

        // Level 3 — a thin tint; the rim below does the rest.
        else -> Modifier.background(surface, shape)
    }

    // The rim: brighter at the top-left, fading out, drawn on top of the surface.
    val rim = Modifier.border(
        width = if (effective == GlassLevel.CHIP) 0.8.dp else 1.dp,
        brush = Brush.linearGradient(listOf(c.glassRimStart, c.glassRimEnd)),
        shape = shape,
    )
    // An inner highlight along the top edge — the thing that makes level 2 read
    // as glass without any GPU work.
    val highlight = if (effective == GlassLevel.CHIP) {
        Modifier
    } else {
        Modifier.background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (c.isDark) 0.10f else 0.34f),
                0.42f to Color.Transparent,
            ),
            shape,
        )
    }

    Box(modifier = modifier.clip(shape).then(glass).then(highlight).then(rim)) {
        content()
    }
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
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
        // Round 9, 1.3: the button is sized by its content, never by its caller.
        // A 40dp pill inside a 48dp touch target, one line, no fixed width.
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 40.dp)
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
            level = GlassLevel.CHIP,
        ) {
            Box(Modifier.padding(contentPadding), contentAlignment = Alignment.Center) {
                // 1.3: one line, always. The old two-line rule is what made a
                // squeezed chip wrap a word letter by letter; now the pill is as
                // wide as its label and the row is what has to give.
                androidx.compose.material3.Text(
                    text = label,
                    color = if (prominent) Color.White else c.text1,
                    style = SippyType.emphasis,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center,
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
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 40.dp)
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
        GlassSurface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            tint = if (selected) c.accent.copy(alpha = 0.72f) else null,
            shadow = false,
            level = GlassLevel.CHIP,
        ) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                androidx.compose.material3.Text(
                    text = label,
                    color = if (selected) Color.White else c.text1,
                    style = SippyType.secondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A stat: a number with its unit, as a chip (round 9, 1.3).
 *
 * Home and Friends both showed a bare number in a pill that had to be guessed at;
 * this is the one shape for "a measurement", content-sized and one line.
 */
@Composable
fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .defaultMinSize(minHeight = if (onClick != null) 40.dp else 36.dp)
            .clip(shape)
            .then(
                if (onClick != null) {
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
            shadow = false,
            level = GlassLevel.CHIP,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = SippyType.emphasis,
                    color = c.text1,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = label,
                    style = SippyType.tab,
                    color = c.text3,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * A row of chips that wraps instead of squeezing (round 9, 1.3): four chips in a
 * 360dp phone do not fit on one line, and forcing them to is what wrapped words
 * letter by letter. Each chip keeps its own width; the extra ones go to a second
 * line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassChipRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Icon-only glass control (round 8, A1): an icon, never a truncated word. The
 * description is for TalkBack only; the icon is drawn by [content].
 */
@Composable
fun GlassIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(Dimens.touch)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(pressScale(interaction))
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            tint = if (selected) c.accent.copy(alpha = 0.72f) else null,
            shadow = false,
            level = GlassLevel.CHIP,
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center, content = content)
        }
    }
}
