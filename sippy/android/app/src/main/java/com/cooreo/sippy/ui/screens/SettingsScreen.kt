package com.cooreo.sippy.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
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
import com.cooreo.sippy.ui.theme.SippyType
import com.cooreo.sippy.ui.theme.toId

@Composable
fun SettingsScreen(
    state: StoredState,
    controller: SippyController,
    onOpenDeveloper: () -> Unit,
    bottomPadding: Dp = 24.dp,
    /** Increments when another screen asks Settings to show Reminders. */
    remindersPrompt: Int = 0,
) {
    val c = LocalSippyColors.current
    val context = LocalContext.current
    var versionTaps by remember { mutableIntStateOf(0) }
    var notice by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var name by remember(state.settings.displayName) {
        androidx.compose.runtime.mutableStateOf(state.settings.displayName)
    }
    val remindersRequester = remember { BringIntoViewRequester() }

    // The bell on Home lands here with the Reminders card in view. The card may
    // still be outside the lazy viewport, so the request never gets in the way.
    LaunchedEffect(remindersPrompt) {
        if (remindersPrompt > 0) runCatching { remindersRequester.bringIntoView() }
    }

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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = SippyType.title, color = c.text1)
        }

        item {
            SettingsCard {
                SectionTitle("Theme")
                Spacer(Modifier.height(10.dp))
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
            }
        }

        item {
            SettingsCard {
                SectionTitle("Accent")
                Spacer(Modifier.height(10.dp))
                // Fixed 2-column grid: every cell is the same width, and the
                // short labels fit one line at 360dp (A4).
                Accent.entries.chunked(2).forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { accent ->
                            Box(Modifier.weight(1f)) {
                                GlassChip(
                                    label = accent.short,
                                    selected = state.settings.accent == accent.toId(),
                                    onClick = { controller.setAccent(accent.toId()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = accent.label },
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SettingsCard {
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
            SettingsCard {
                SectionTitle("Your name")
                Text(
                    text = "Only used for the Home greeting. Leave it empty to hide it.",
                    color = c.text3,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    label = "Save name",
                    onClick = { controller.setDisplayName(name.trim()) },
                    enabled = name.trim() != state.settings.displayName,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            SettingsCard(modifier = Modifier.bringIntoViewRequester(remindersRequester)) {
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
            SettingsCard {
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
            SettingsCard {
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
            SettingsCard {
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

/** One title style for every section, on the app's Inter scale. */
@Composable
private fun SectionTitle(text: String) {
    val c = LocalSippyColors.current
    Text(text = text, style = SippyType.emphasis, color = c.text1)
}

/** Site glass, 16dp inside: the single card shape every Settings section uses. */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        content = content,
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
