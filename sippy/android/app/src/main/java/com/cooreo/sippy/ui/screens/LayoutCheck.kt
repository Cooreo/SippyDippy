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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassIconButton
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * Developer options → "Layout check" (round 8, A1).
 *
 * Every button style rendered with its real label at font scale 1.0 and 1.5. If a
 * label ever ellipsises again, it is visible here without hunting through screens,
 * which is the whole point: the bug was invisible until a screenshot showed "S…".
 */
@Composable
fun LayoutCheckCard() {
    val c = LocalSippyColors.current
    GlassCard {
        Column {
            Text("Layout check", color = c.text1, fontSize = 15.sp)
            Text(
                text = "Every button style with its real label at font scale 1.0 and 1.5. " +
                    "Nothing may ellipsise; nothing may clip.",
                color = c.text3,
                fontSize = 11.sp,
            )
            listOf(1.0f, 1.5f).forEach { scale ->
                Spacer(Modifier.height(10.dp))
                Text("font scale $scale", color = c.text2, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, scale),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(label = "Shop", onClick = {})
                            GlassChip(label = "See all", onClick = {})
                            GlassChip(label = "Back", selected = true, onClick = {})
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(label = "−250", onClick = {})
                            GlassChip(label = "+250", onClick = {})
                            GlassChip(label = "−250", onClick = {})
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(label = "Buy 30", onClick = {})
                            GlassChip(label = "Equip", onClick = {})
                            GlassChip(label = "Needs Koi", onClick = {})
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(w, h),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = c.text1,
                                        start = androidx.compose.ui.geometry.Offset(w, 0f),
                                        end = androidx.compose.ui.geometry.Offset(0f, h),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                            Text("icon-only uses an icon", color = c.text3, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
