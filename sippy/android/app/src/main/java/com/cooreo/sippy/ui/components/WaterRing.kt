package com.cooreo.sippy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.ui.theme.LocalSippyColors
import kotlin.math.roundToInt

/**
 * The glass water ring: goal fraction, animated on a spring, with the counting
 * millilitre readout in the middle. Declarative animations only — the system
 * "remove animations" setting and the in-app Reduce-effects switch collapse
 * them by passing [animate] = false.
 */
@Composable
fun WaterRing(
    hydratedMl: Int,
    goalMl: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    size: Int = 260,
) {
    val c = LocalSippyColors.current
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (animate) {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
        label = "ringFraction",
    )
    val animatedMl by animateIntAsState(
        targetValue = hydratedMl,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "ringMl",
    )
    val shownMl = if (animate) animatedMl else hydratedMl
    val shownFraction = if (animate) animatedFraction else fraction

    Box(
        modifier = modifier
            .size(size.dp)
            .semantics {
                contentDescription =
                    "$shownMl of $goalMl millilitres, ${(shownFraction * 100).roundToInt()} percent of today's goal"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.072f
            val inset = stroke / 2f + this.size.minDimension * 0.03f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // glass well
            drawArc(
                brush = Brush.verticalGradient(
                    listOf(c.glassInner, c.glassTint),
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // water
            if (shownFraction > 0.001f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(c.accent, c.accentBright, c.accent),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * shownFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            // inner highlight
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(this.size.width * 0.38f, this.size.height * 0.30f),
                    radius = this.size.minDimension * 0.55f,
                ),
                radius = this.size.minDimension * 0.40f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = shownMl.toString(),
                color = c.text1,
                fontSize = (size / 7).sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "of $goalMl ml",
                color = c.text2,
                fontSize = 14.sp,
                modifier = Modifier.width(160.dp),
            )
        }
    }
}
