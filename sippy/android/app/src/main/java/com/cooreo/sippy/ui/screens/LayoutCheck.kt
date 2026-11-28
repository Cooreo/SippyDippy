package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassChipRow
import com.cooreo.sippy.ui.glass.GlassIconButton
import com.cooreo.sippy.ui.glass.StatChip
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType

/**
 * Developer options → "Layout check" (round 8, A1, updated round 9 1.3).
 *
 * Every button and chip style rendered with its real label at font scales 1.0,
 * 1.3 and 1.5. If a label ever ellipsises or wraps inside a pill again, it is
 * visible here without hunting through screens.
 */
@Composable
fun LayoutCheckCard() {
    val c = LocalSippyColors.current
    GlassCard {
        Column {
            Text("Layout check", style = SippyType.emphasis, color = c.text1)
            Text(
                text = "Every button and chip style with its real label at font scale 1.0, " +
                    "1.3 and 1.5. Nothing may ellipsise; nothing may clip.",
                style = SippyType.tab,
                color = c.text3,
            )
            listOf(1.0f, 1.3f, 1.5f).forEach { scale ->
                Spacer(Modifier.height(10.dp))
                Text("font scale $scale", style = SippyType.tab, color = c.text2)
                Spacer(Modifier.height(6.dp))
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, scale),
                ) {
                    LayoutCheckSampleSet()
                }
            }
        }
    }
}

/** Extracted so both [LayoutCheckCard] and instrumentation tests check the exact same pills. */
@Composable
fun LayoutCheckSampleSet() {
    val c = LocalSippyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassChipRow {
            GlassChip(label = "Shop", onClick = {})
            GlassChip(label = "See all", onClick = {})
            GlassChip(label = "Back", selected = true, onClick = {})
            GlassChip(label = "−250", onClick = {})
            GlassChip(label = "+250", onClick = {})
            GlassChip(label = "−15 min", onClick = {})
            GlassChip(label = "Buy", onClick = {})
            GlassChip(label = "Equip", onClick = {})
            GlassChip(label = "Needs Koi", onClick = {})
        }
        GlassChipRow {
            StatChip(value = "7", label = "days")
            StatChip(value = "120", label = "pts", onClick = {})
        }
        GlassChipRow {
            GlassButton(label = "Dress up", onClick = {})
            GlassButton(label = "Save", onClick = {}, prominent = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(contentDescription = "Close", onClick = {}) {
                Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = c.text1,
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = c.text1,
                        start = Offset(w, 0f),
                        end = Offset(0f, h),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Text("icon-only uses an icon", style = SippyType.tab, color = c.text3)
        }
    }
}
