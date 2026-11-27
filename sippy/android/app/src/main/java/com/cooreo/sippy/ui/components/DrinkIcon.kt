package com.cooreo.sippy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyColors

/** Drink families, used only for the icon tint — the drink table stays canonical. */
enum class DrinkGroup { WATER, TEA, COFFEE, MILK, JUICE, SODA, ALCOHOL, SOUP }

object DrinkGroups {

    private val groups: Map<String, DrinkGroup> = mapOf(
        "water" to DrinkGroup.WATER,
        "sparkling" to DrinkGroup.WATER,
        "mineral" to DrinkGroup.WATER,
        "coconut" to DrinkGroup.WATER,
        "electrolyte" to DrinkGroup.WATER,
        "flavoured-water" to DrinkGroup.WATER,
        "tea" to DrinkGroup.TEA,
        "green-tea" to DrinkGroup.TEA,
        "herbal-tea" to DrinkGroup.TEA,
        "iced-tea" to DrinkGroup.TEA,
        "kombucha" to DrinkGroup.TEA,
        "coffee" to DrinkGroup.COFFEE,
        "espresso" to DrinkGroup.COFFEE,
        "latte" to DrinkGroup.COFFEE,
        "milk" to DrinkGroup.MILK,
        "oat-milk" to DrinkGroup.MILK,
        "almond-milk" to DrinkGroup.MILK,
        "kefir" to DrinkGroup.MILK,
        "juice" to DrinkGroup.JUICE,
        "apple-juice" to DrinkGroup.JUICE,
        "lemonade" to DrinkGroup.JUICE,
        "smoothie" to DrinkGroup.JUICE,
        "soda" to DrinkGroup.SODA,
        "diet-soda" to DrinkGroup.SODA,
        "sports" to DrinkGroup.SODA,
        "energy" to DrinkGroup.SODA,
        "beer" to DrinkGroup.ALCOHOL,
        "wine" to DrinkGroup.ALCOHOL,
        "cocktail" to DrinkGroup.ALCOHOL,
        "soup" to DrinkGroup.SOUP,
    )

    fun of(drinkId: String): DrinkGroup = groups[drinkId] ?: DrinkGroup.WATER

    /** Every drink id with an explicit group; a test keeps it in step with the table. */
    val knownIds: Set<String> get() = groups.keys

    /**
     * Tints stay inside the app's palette — blue, lagoon green, violet and the
     * two text tones — so a drink icon can never introduce an off-brand colour.
     */
    fun tint(group: DrinkGroup, colors: SippyColors): Color = when (group) {
        DrinkGroup.WATER -> colors.accent
        DrinkGroup.TEA -> colors.ok
        DrinkGroup.COFFEE -> colors.text2
        DrinkGroup.MILK -> colors.text1
        DrinkGroup.JUICE -> Color(0xFFA78BFA)
        DrinkGroup.SODA -> colors.accentBright
        DrinkGroup.ALCOHOL -> colors.text3
        DrinkGroup.SOUP -> colors.ok
    }
}

/**
 * A drink's icon: a glass disc with a tinted droplet. Drawn, not an image, so it
 * scales to any size and keeps the app free of new art assets.
 */
@Composable
fun DrinkIcon(
    drinkId: String,
    size: Dp,
    /** TalkBack text; null when the surrounding row already describes the drink. */
    label: String? = null,
) {
    val c = LocalSippyColors.current
    val tint = DrinkGroups.tint(DrinkGroups.of(drinkId), c)
    Canvas(
        modifier = Modifier
            .size(size)
            .semantics { if (label != null) contentDescription = label },
    ) {
        val radius = this.size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.glassInner, c.glassTint.copy(alpha = 0.10f)),
                center = Offset(this.size.width * 0.35f, this.size.height * 0.25f),
                radius = radius * 1.3f,
            ),
            radius = radius,
        )
        drawCircle(
            color = c.glassRimStart.copy(alpha = 0.35f),
            radius = radius - 0.5.dp.toPx(),
            style = Stroke(width = 1.dp.toPx()),
        )

        val dropWidth = this.size.width * 0.30f
        val dropHeight = this.size.height * 0.46f
        val left = (this.size.width - dropWidth) / 2f
        val top = (this.size.height - dropHeight) / 2f
        val droplet = Path().apply {
            val cx = left + dropWidth / 2f
            moveTo(cx, top)
            cubicTo(
                cx + dropWidth * 0.44f, top + dropHeight * 0.40f,
                left + dropWidth, top + dropHeight * 0.66f,
                cx, top + dropHeight,
            )
            cubicTo(
                left, top + dropHeight * 0.66f,
                cx - dropWidth * 0.44f, top + dropHeight * 0.40f,
                cx, top,
            )
            close()
        }
        drawPath(droplet, tint.copy(alpha = 0.92f))
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = dropWidth * 0.13f,
            center = Offset(left + dropWidth * 0.36f, top + dropHeight * 0.55f),
        )
    }
}
