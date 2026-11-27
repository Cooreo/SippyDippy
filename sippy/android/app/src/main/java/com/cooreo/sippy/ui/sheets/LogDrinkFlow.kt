package com.cooreo.sippy.ui.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.drinks.DrinkCategory
import com.cooreo.sippy.domain.water.VolumeRules
import com.cooreo.sippy.model.Drink
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassLevel
import com.cooreo.sippy.ui.glass.LocalGlassLevel
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.DrinkTints
import com.cooreo.sippy.ui.theme.LocalSippyColors
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Log flow behind the + button (round 8, B7).
 *
 * The button's own glass grows into this sheet — a container transform, not a
 * slide: the sheet is laid out at its final size and then scaled and translated
 * so that at progress 0 it sits exactly on the button's rectangle. Drinks rise
 * into a scrollable row of bubbles with an 80 ms stagger (capped at six, so the
 * whole opening stays inside the 700 ms budget), a glass preview fills as the
 * volume changes, and the three presets sit above the slider.
 *
 * Confirming shrinks the sheet back into the button and hands over to the caller,
 * which logs the drink and flies the droplet into the orb.
 *
 * Under Reduce effects every step is instant: no transform, no stagger, no flight.
 */
@Composable
fun LogDrinkFlow(
    /** The + button's rectangle in root coordinates; Rect.Zero when unknown. */
    origin: Rect,
    animate: Boolean,
    onDismiss: () -> Unit,
    onLog: (Drink, Int) -> Unit,
    onCustom: () -> Unit,
) {
    val c = LocalSippyColors.current
    val drinks = remember { com.cooreo.sippy.domain.drinks.DrinkTable.drinks }
    var selected by remember { mutableStateOf(drinks.first()) }
    var volume by remember { mutableIntStateOf(selected.defaultVolumeMl.coerceIn(100, 600)) }
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    var panel by remember { mutableStateOf(Rect.Zero) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (animate) progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    // Leaving always shrinks first, then hands control back: the caller removes
    // this composable, so the animation has to finish while it still exists.
    fun leave(then: () -> Unit) {
        if (busy) return
        busy = true
        if (!animate) {
            then()
            return
        }
        scope.launch {
            progress.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            then()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The scrim deepens as the sheet arrives, and takes the dismiss tap.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * 0.46f }
                .background(Color.Black)
                .clickable(enabled = !busy) { leave(onDismiss) },
        )

        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Measured without a layer of its own, so `panel` is the sheet's real
            // rectangle and not its transformed one.
            Box(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { panel = it.boundsInRoot() },
            ) {
                CompositionLocalProvider(LocalGlassLevel provides GlassLevel.LIVE) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val p = if (animate) progress.value.coerceIn(0f, 1f) else 1f
                                if (origin != Rect.Zero && panel != Rect.Zero) {
                                    val fromX = origin.width / panel.width
                                    val fromY = origin.height / panel.height
                                    scaleX = fromX + (1f - fromX) * p
                                    scaleY = fromY + (1f - fromY) * p
                                    translationX = (origin.center.x - panel.center.x) * (1f - p)
                                    translationY = (origin.center.y - panel.center.y) * (1f - p)
                                }
                                // At a button's size the text would be unreadable,
                                // so the surface fades up as it grows.
                                alpha = 0.25f + 0.75f * p
                            },
                        shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Log a drink",
                                    color = c.text1,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "Close",
                                    color = c.text3,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable(enabled = !busy) { leave(onDismiss) }
                                        .padding(8.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                drinks.forEachIndexed { index, drink ->
                                    BubbleChoice(
                                        drink = drink,
                                        selected = drink.id == selected.id,
                                        animate = animate,
                                        index = index,
                                        onClick = {
                                            selected = drink
                                            volume = drink.defaultVolumeMl.coerceIn(100, 600)
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassPreview(
                                    drink = selected,
                                    volume = volume,
                                    fullMl = 600,
                                )
                                Spacer(Modifier.width(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "$volume ml",
                                        color = c.text1,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Light,
                                    )
                                    Text(
                                        text = selected.name,
                                        color = c.text2,
                                        fontSize = 13.sp,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        VolumeRules.PRESETS.forEach { preset ->
                                            GlassChip(
                                                label = "$preset",
                                                selected = volume == preset,
                                                onClick = { volume = preset },
                                            )
                                        }
                                    }
                                }
                            }
                            Slider(
                                value = volume.toFloat(),
                                onValueChange = { volume = it.roundToInt().coerceIn(100, 600) },
                                valueRange = 100f..600f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(4.dp))
                            GlassButton(
                                label = "Add $volume ml of ${selected.name}",
                                prominent = true,
                                onClick = { leave { onLog(selected, volume) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            GlassChip(
                                label = "Custom…",
                                onClick = { leave(onCustom) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One drink, as a bubble that rises into place. The stagger is capped at six
 * bubbles (480 ms), so adding a seventh category can never push the opening past
 * the 700 ms the round allows.
 */
@Composable
private fun BubbleChoice(
    drink: Drink,
    selected: Boolean,
    animate: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val c = LocalSippyColors.current
    val rise = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(drink.id, animate) {
        if (!animate) {
            rise.snapTo(1f)
            return@LaunchedEffect
        }
        delay((min(index, 5) * 80).toLong())
        rise.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                translationY = (1f - rise.value) * 26f * density
                val scale = 0.72f + 0.28f * rise.value
                scaleX = scale
                scaleY = scale
                alpha = rise.value
            }
            .clip(CircleShape)
            .background(
                if (selected) c.accent.copy(alpha = 0.30f) else c.glassTint.copy(alpha = 0.18f),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) c.accentBright else c.glassRimStart.copy(alpha = 0.45f),
                shape = CircleShape,
            )
            .clickable(enabled = !selected) { onClick() }
            .semantics { contentDescription = drink.name },
        contentAlignment = Alignment.Center,
    ) {
        DrinkIcon(drinkId = drink.id, size = 34.dp)
    }
}

/**
 * The glass that fills as the slider moves: a tapered tumbler, tinted by the drink
 * family, level set by the chosen volume against a 600 ml full glass. Drawn, so it
 * costs nothing to animate.
 */
@Composable
private fun GlassPreview(drink: Drink, volume: Int, fullMl: Int) {
    val tint = DrinkTints.of(DrinkCategory.of(drink.id))
    val fraction = (volume.toFloat() / fullMl.toFloat()).coerceIn(0.04f, 1f)
    Canvas(Modifier.size(width = 58.dp, height = 88.dp)) {
        val inset = size.width * 0.10f
        val glass = Path().apply {
            moveTo(inset, 0f)
            lineTo(size.width - inset, 0f)
            lineTo(size.width - inset * 0.35f, size.height)
            lineTo(inset * 0.35f, size.height)
            close()
        }
        val levelTop = size.height * (1f - fraction)
        clipPath(glass) {
            drawRect(
                color = tint.copy(alpha = 0.88f),
                topLeft = Offset(0f, levelTop),
                size = Size(size.width, size.height - levelTop),
            )
            // A brighter band at the surface, so the level reads as liquid.
            drawRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(0f, levelTop),
                size = Size(size.width, size.height * 0.02f),
            )
        }
        drawPath(glass, Color.White.copy(alpha = 0.55f), style = Stroke(width = 1.6.dp.toPx()))
    }
}
