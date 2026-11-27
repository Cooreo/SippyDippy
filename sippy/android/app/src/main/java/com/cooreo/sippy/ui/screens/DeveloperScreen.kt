package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * Developer options (7 taps on the version in Settings). Everything here is
 * throwaway state or the real reset path — nothing hidden can corrupt data.
 */
@Composable
fun DeveloperScreen(
    state: StoredState,
    controller: SippyController,
    onClose: () -> Unit,
) {
    val c = LocalSippyColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Developer", color = c.text1, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    GlassChip(label = "Close", onClick = onClose)
                }
            }
            item {
                GlassCard {
                    Text("Friend moods", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Mood.entries.forEach { mood ->
                            Box(Modifier.weight(1f)) {
                                GlassChip(
                                    label = mood.name.lowercase(),
                                    onClick = { controller.setMoodOverride(mood) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassChip(label = "Clear mood override", onClick = { controller.setMoodOverride(null) })
                }
            }
            item {
                GlassCard {
                    Text("Ring preview", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0f, 0.5f, 1f).forEach { value ->
                            Box(Modifier.weight(1f)) {
                                GlassChip(
                                    label = "${(value * 100).toInt()}%",
                                    onClick = { controller.setRingOverride(value) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            GlassChip(
                                label = "Live",
                                onClick = { controller.setRingOverride(null) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassChip(label = "Replay celebration", onClick = { controller.triggerCelebration() })
                }
            }
            item {
                GlassCard {
                    Text("Updater log (last 20)", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    if (state.update.log.isEmpty()) {
                        Text("No entries yet.", color = c.text3, fontSize = 12.sp)
                    } else {
                        state.update.log.takeLast(20).forEach { line ->
                            Text(line, color = c.text2, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Backups: " + controller.backupFiles().take(3).joinToString(", ")
                            .ifEmpty { "none" },
                        color = c.text3,
                        fontSize = 11.sp,
                    )
                }
            }
            item {
                GlassCard {
                    Text("Danger zone", color = c.danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Reset removes the log, streak, friends and settings.",
                        color = c.text3,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        label = "Reset all data",
                        onClick = { controller.resetAllData() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
