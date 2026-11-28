package com.cooreo.sippy.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StartupAnimation
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassChipRow
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.Accent
import com.cooreo.sippy.ui.theme.Dimens
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
    var notice by remember { mutableStateOf<String?>(null) }
    var name by remember(state.settings.displayName) {
        mutableStateOf(state.settings.displayName)
    }
    val remindersRequester = remember { BringIntoViewRequester() }

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
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = 28.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "title") {
            Text("Settings", style = SippyType.title, color = c.text1)
        }

        item(key = "hydration") {
            SettingsCard {
                GroupHeader("Hydration")
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Daily goal",
                    subtitle = "Allowed ${Settings.MIN_GOAL_ML}–${Settings.MAX_GOAL_ML} ml " +
                        "(default ${Settings.DEFAULT_GOAL_ML}).",
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassChip(
                        label = "−250",
                        onClick = { controller.setGoal(state.settings.goalMl - 250) },
                    )
                    Text(
                        text = "${state.settings.goalMl} ml",
                        style = SippyType.section,
                        color = c.text1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    GlassChip(
                        label = "+250",
                        onClick = { controller.setGoal(state.settings.goalMl + 250) },
                    )
                }
                SettingsDivider()
                SettingsRow(
                    title = "Reminders",
                    subtitle = if (state.settings.remindersEnabled) {
                        "Up to ${Settings.MAX_REMINDERS} a day; skipped once the goal is met."
                    } else {
                        "Off. Turn on to get gentle sips during the day."
                    },
                    modifier = Modifier.bringIntoViewRequester(remindersRequester),
                    onClick = { enableReminders(!state.settings.remindersEnabled) },
                    trailing = {
                        Switch(
                            checked = state.settings.remindersEnabled,
                            onCheckedChange = { enableReminders(it) },
                        )
                    },
                )
                if (state.settings.remindersEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.settings.reminderTimes
                            .take(Settings.MAX_REMINDERS)
                            .forEachIndexed { slot, time ->
                                SettingsRow(
                                    title = "Reminder ${slot + 1}",
                                    trailing = {
                                        TimeField(
                                            value = time,
                                            onChange = { controller.setReminderTime(slot, it) },
                                        )
                                    },
                                )
                            }
                    }
                }
            }
        }

        item(key = "appearance") {
            SettingsCard {
                GroupHeader("Appearance & feel")
                Spacer(Modifier.height(10.dp))
                SettingsRow(title = "Theme")
                Spacer(Modifier.height(6.dp))
                GlassChipRow {
                    ThemeMode.entries.forEach { mode ->
                        GlassChip(
                            label = mode.label(),
                            selected = state.settings.theme == mode,
                            onClick = { controller.setTheme(mode) },
                        )
                    }
                }
                SettingsDivider()
                SettingsRow(title = "Accent")
                Spacer(Modifier.height(6.dp))
                GlassChipRow {
                    Accent.entries.forEach { accent ->
                        GlassChip(
                            label = accent.short,
                            selected = state.settings.accent == accent.toId(),
                            onClick = { controller.setAccent(accent.toId()) },
                            modifier = Modifier.semantics { contentDescription = accent.label },
                        )
                    }
                }
                SettingsDivider()
                SettingsRow(
                    title = "Startup animation",
                    subtitle = "A friend arrives in front of the mark, then the app fades up " +
                        "behind it. Full plays the whole arrival, Short cuts the flight, Off " +
                        "goes straight to Home.",
                )
                Spacer(Modifier.height(8.dp))
                GlassChipRow {
                    StartupAnimation.entries.forEach { mode ->
                        GlassChip(
                            label = when (mode) {
                                StartupAnimation.FULL -> "Full"
                                StartupAnimation.SHORT -> "Short"
                                StartupAnimation.OFF -> "Off"
                            },
                            selected = state.settings.startupAnimation == mode,
                            onClick = { controller.setStartupAnimation(mode) },
                        )
                    }
                }
                SettingsDivider()
                SettingsRow(
                    title = "Reduce effects",
                    subtitle = "Turns off blur, refraction and animation everywhere.",
                    onClick = { controller.setReduceEffects(!state.settings.reduceEffects) },
                    trailing = {
                        Switch(
                            checked = state.settings.reduceEffects,
                            onCheckedChange = { controller.setReduceEffects(it) },
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    title = "Haptics",
                    subtitle = "A tick on tab changes, a thud when you log, and a two-beat " +
                        "pattern when you reach the goal.",
                    onClick = { controller.setHaptics(!state.settings.hapticsEnabled) },
                    trailing = {
                        Switch(
                            checked = state.settings.hapticsEnabled,
                            onCheckedChange = { controller.setHaptics(it) },
                        )
                    },
                )
            }
        }

        item(key = "profile") {
            SettingsCard {
                GroupHeader("Profile")
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Your name",
                    subtitle = "Only used for the Home greeting. Leave it empty to hide it.",
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text("Name", style = SippyType.caption) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    label = "Save",
                    onClick = {
                        controller.setDisplayName(name.trim())
                        notice = "Greeting name updated."
                    },
                    enabled = name.trim() != state.settings.displayName,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "app") {
            SettingsCard {
                GroupHeader("App & updates")
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Updates",
                    subtitle = "Sippy installs updates itself from GitHub Releases and checks " +
                        "the signature before installing anything.",
                )
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    label = "Check for updates",
                    onClick = { controller.checkForUpdate(manual = true) },
                    modifier = Modifier.fillMaxWidth(),
                )
                controller.updateMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = SippyType.secondary, color = c.text2)
                }
                SettingsDivider()
                SettingsRow(
                    title = "Sippy ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    subtitle = "${DrinkTable.drinks.size} drinks in the table. " +
                        "No accounts, no analytics, no Play Store.",
                    onClick = {
                        versionTaps += 1
                        if (versionTaps >= 7 && !state.settings.developerUnlocked) {
                            controller.setDeveloperUnlocked(true)
                            notice = "Developer options unlocked."
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        label = "Developer options",
                        onClick = onOpenDeveloper,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        notice?.let { text ->
            item(key = "notice") {
                Text(text, style = SippyType.secondary, color = c.text2)
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    val c = LocalSippyColors.current
    Text(text = title, style = SippyType.section, color = c.text1)
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalSippyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.defaultMinSize(minHeight = Dimens.touch) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = SippyType.emphasis, color = c.text1)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, style = SippyType.secondary, color = c.text3)
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun SettingsDivider() {
    val c = LocalSippyColors.current
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.glassRimEnd),
    )
    Spacer(Modifier.height(10.dp))
}

/** Site glass, 16dp inside: the single card shape every Settings group uses. */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        content = content,
    )
}

@Composable
private fun TimeField(value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.take(5)
            if (Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(text)) onChange(text)
        },
        singleLine = true,
        modifier = Modifier.width(116.dp),
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
