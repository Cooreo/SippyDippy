package com.cooreo.sippy.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.AccentId
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.Accent
import com.cooreo.sippy.ui.theme.LocalSippyColors

@Composable
fun SettingsScreen(
    state: StoredState,
    controller: SippyController,
    onOpenDeveloper: () -> Unit,
) {
    val c = LocalSippyColors.current
    val context = LocalContext.current
    var versionTaps by remember { mutableIntStateOf(0) }
    var notice by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            controller.setRemindersEnabled(true)
            notice = "Reminders are on."
        } else {
            notice = "Notifications are blocked, so reminders stay off."
        }
    }

    fun enableReminders(enabled: Boolean) {
        controller.setRemindersEnabled(enabled)
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", color = c.text1, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        item {
            GlassCard {
                SectionTitle("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        Box(Modifier.weight(1f)) {
                            GlassChip(
                                label = mode.label(),
                                selected = state.settings.theme == mode,
                                onClick = { controller.setTheme(mode) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionTitle("Accent")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accent.entries.forEach { accent ->
                        Box(Modifier.weight(1f)) {
                            GlassChip(
                                label = accent.label,
                                selected = state.settings.accent == accent.toId(),
                                onClick = { controller.setAccent(accent.toId()) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                SectionTitle("Daily goal")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassChip(
                        label = "−250",
                        onClick = { controller.setGoal(state.settings.goalMl - 250) },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${state.settings.goalMl} ml",
                        color = c.text1,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    GlassChip(
                        label = "+250",
                        onClick = { controller.setGoal(state.settings.goalMl + 250) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Allowed ${Settings.MIN_GOAL_ML}–${Settings.MAX_GOAL_ML} ml " +
                        "(default ${Settings.DEFAULT_GOAL_ML}).",
                    color = c.text3,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Reminders")
                        Text(
                            text = if (state.settings.remindersEnabled) {
                                "Up to ${Settings.MAX_REMINDERS} a day; skipped once the goal is met."
                            } else {
                                "Off. Turn on to get gentle sips during the day."
                            },
                            color = c.text3,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = state.settings.remindersEnabled,
                        onCheckedChange = { enableReminders(it) },
                    )
                }
                if (state.settings.remindersEnabled) {
                    Spacer(Modifier.height(10.dp))
                    state.settings.reminderTimes.take(Settings.MAX_REMINDERS).forEachIndexed { slot, time ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reminder ${slot + 1}", color = c.text2, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            TimeField(value = time, onChange = { controller.setReminderTime(slot, it) })
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Reduce effects")
                        Text(
                            text = "Turns off blur, refraction and animation everywhere.",
                            color = c.text3,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = state.settings.reduceEffects,
                        onCheckedChange = { controller.setReduceEffects(it) },
                    )
                }
            }
        }

        item {
            GlassCard {
                SectionTitle("Updates")
                Text(
                    text = "Sippy installs updates itself from GitHub Releases and checks the " +
                        "signature before installing anything.",
                    color = c.text3,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    label = "Check for updates",
                    onClick = { controller.checkForUpdate(manual = true) },
                    modifier = Modifier.fillMaxWidth(),
                )
                controller.updateMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = c.text2, fontSize = 12.sp)
                }
            }
        }

        item {
            GlassCard {
                SectionTitle("About")
                Text("Sippy ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = c.text2,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .androidClickable {
                            versionTaps += 1
                            if (versionTaps >= 7 && !state.settings.developerUnlocked) {
                                controller.setDeveloperUnlocked(true)
                                notice = "Developer options unlocked."
                            }
                        },
                )
                Text(
                    text = "${DrinkTable.drinks.size} drinks in the table. " +
                        "No accounts, no analytics, no Play Store.",
                    color = c.text3,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    label = "Source & releases",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Cooreo/SippyDippy"),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.settings.developerUnlocked) {
                    Spacer(Modifier.height(10.dp))
                    GlassButton(
                        label = "Developer options",
                        onClick = onOpenDeveloper,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        notice?.let { text ->
            item {
                Text(text, color = c.text2, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val c = LocalSippyColors.current
    Text(
        text = text,
        color = c.text1,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TimeField(value: String, onChange: (String) -> Unit) {
    var text by remember(value) { androidx.compose.runtime.mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.take(5)
            if (Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(text)) onChange(text)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.45f),
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
