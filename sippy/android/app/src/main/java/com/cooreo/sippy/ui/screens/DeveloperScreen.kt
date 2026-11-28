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
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.intro.Vignette
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassChipRow
import com.cooreo.sippy.ui.glass.GlassDiagnostics
import com.cooreo.sippy.ui.glass.LocalBackdrop
import com.cooreo.sippy.ui.glass.LocalGlassTier
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.system.rememberFrameStats
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType

/**
 * Developer options (7 taps on the version in Settings). Everything here is
 * throwaway state or the real reset path — nothing hidden can corrupt data.
 */
@Composable
fun DeveloperScreen(
    state: StoredState,
    controller: SippyController,
    onClose: () -> Unit,
    diagnostics: GlassDiagnostics,
    /** B8: replay the intro, or replay one specific vignette. */
    onReplayIntro: () -> Unit = {},
    onPickVignette: (Vignette?) -> Unit = {},
) {
    val c = LocalSippyColors.current
    // The tier supplied at the root plus the live presence of the backdrop
    // layer: together they answer "why does it look flat?" (A2).
    val activeTier = LocalGlassTier.current
    val backdropReady = LocalBackdrop.current != null
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
                    Text("Developer", style = SippyType.title, color = c.text1)
                    Spacer(Modifier.weight(1f))
                    GlassChip(label = "Close", onClick = onClose)
                }
            }
            item {
                GlassCard {
                    Text("Glass", style = SippyType.emphasis, color = c.text1)
                    Spacer(Modifier.height(6.dp))
                    DevRow("Active tier", activeTier.name + " \u2014 " + diagnostics.tierReason)
                    DevRow("Motion", if (diagnostics.motionInstant) "instant \u2014 " + diagnostics.motionReason else "animated")
                    DevRow("API level", diagnostics.apiLevel.toString())
                    DevRow("Reduce effects (app)", if (diagnostics.reduceEffects) "on" else "off")
                    DevRow("System animations", if (diagnostics.systemAnimationsOff) "off" else "on")
                    DevRow("Low-RAM device", if (diagnostics.lowRamDevice) "yes" else "no")
                    DevRow("RenderEffect (31+)", if (diagnostics.renderEffectSupported) "supported" else "missing")
                    DevRow("Runtime shaders (33+)", if (diagnostics.runtimeShaderSupported) "supported" else "missing")
                    DevRow("Backdrop layer", if (backdropReady) "initialised" else "NOT initialised")
                    Spacer(Modifier.height(8.dp))
                    // B1: the switch that answers "is the lens really running?".
                    GlassChip(
                        label = if (controller.glassBoost) {
                            "Exaggerate glass: ON (x2.4)"
                        } else {
                            "Exaggerate glass: OFF"
                        },
                        selected = controller.glassBoost,
                        onClick = { controller.glassBoost = !controller.glassBoost },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Boosts the blur radius and the lens refraction by 2.4x so the " +
                            "effect is obvious on a still phone screen.",
                        style = SippyType.tab,
                        color = c.text3,
                    )
                }
            }

            item {
                GlassCard {
                    Text("Friend moods", style = SippyType.emphasis, color = c.text1)
                    Spacer(Modifier.height(8.dp))
                    GlassChipRow {
                        Mood.entries.forEach { mood ->
                            GlassChip(
                                label = mood.name.lowercase(),
                                onClick = { controller.previewMood(mood) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassChip(label = "Clear mood override", onClick = { controller.previewMood(null) })
                }
            }
            item {
                GlassCard {
                    Text("Ring preview", style = SippyType.emphasis, color = c.text1)
                    Spacer(Modifier.height(8.dp))
                    GlassChipRow {
                        listOf(0f, 0.5f, 1f).forEach { value ->
                            GlassChip(
                                label = "${(value * 100).toInt()}%",
                                onClick = { controller.previewRing(value) },
                            )
                        }
                        GlassChip(
                            label = "Live",
                            onClick = { controller.previewRing(null) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassChip(label = "Replay celebration", onClick = { controller.triggerCelebration() })
                }
            }
            item {
                GlassCard {
                    Text("Updater log (last 20)", style = SippyType.emphasis, color = c.text1)
                    Spacer(Modifier.height(6.dp))
                    if (state.update.log.isEmpty()) {
                        Text("No entries yet.", style = SippyType.caption, color = c.text3)
                    } else {
                        state.update.log.takeLast(20).forEach { line ->
                            Text(line, style = SippyType.tab, color = c.text2)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Backups: " + controller.backupFiles().take(3).joinToString(", ")
                            .ifEmpty { "none" },
                        style = SippyType.tab,
                        color = c.text3,
                    )
                }
            }
            item {
                // B8: replay the intro without restarting the app.
                GlassCard {
                    Text(
                        "Startup animation",
                        style = SippyType.emphasis,
                        color = c.text1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Currently ${state.settings.startupAnimation.name.lowercase()}. " +
                            "Replaying keeps the hand-off into Home, so nothing goes black.",
                        style = SippyType.tab,
                        color = c.text3,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassButton(
                        label = "Replay intro",
                        onClick = onReplayIntro,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassChipRow {
                        GlassChip(
                            label = "Random",
                            selected = false,
                            onClick = { onPickVignette(null) },
                        )
                        Vignette.ALL.forEach { vignette ->
                            GlassChip(
                                label = vignette.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = false,
                                onClick = { onPickVignette(vignette) },
                            )
                        }
                    }
                }
            }
            item {
                // B10: real display cadence, measured only while this screen is up.
                val frames = rememberFrameStats(enabled = true)
                GlassCard {
                    Text(
                        "Frame timing",
                        style = SippyType.emphasis,
                        color = c.text1,
                    )
                    Spacer(Modifier.height(6.dp))
                    DevRow("Frames per second", frames.fps.toString())
                    DevRow(
                        "Worst frame (last 1s)",
                        if (frames.worstFrameMs <= 0f) "—" else "${frames.worstFrameMs.toInt()} ms",
                    )
                    DevRow("Frames counted", frames.frames.toString())
                    Text(
                        text = "Measured with the Choreographer while this screen is open. " +
                            "16.7 ms is a 60 Hz budget, 8.3 ms a 120 Hz one.",
                        style = SippyType.tab,
                        color = c.text3,
                    )
                }
            }
            item {
                AnchorTunerCard()
            }
            item {
                LayoutCheckCard()
            }
            item {
                GlassCard {
                    Text("Danger zone", style = SippyType.emphasis, color = c.danger)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Reset removes the log, streak, friends and settings.",
                        style = SippyType.tab,
                        color = c.text3,
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

/** One label/value line in the developer cards. */
@Composable
private fun DevRow(label: String, value: String) {
    val c = LocalSippyColors.current
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = SippyType.caption, color = c.text2, modifier = Modifier.weight(1f))
        Text(value, style = SippyType.caption, color = c.text1)
    }
}
