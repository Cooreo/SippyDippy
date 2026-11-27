package com.cooreo.sippy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * A log row's share of the daily goal, as a 3dp bar (round 8, B4). Filled with
 * the row's own tint so a day of tea and coffee is readable at a glance.
 */
@Composable
fun ShareBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    color: Color? = null,
) {
    val c = LocalSippyColors.current
    val fill = fraction.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fill)
                .height(height)
                .clip(shape)
                .background(color ?: c.accentBright),
        )
    }
}
