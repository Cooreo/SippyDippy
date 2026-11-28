package com.cooreo.sippy.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.water.VolumeRules
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassChipRow
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val sheetTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Bottom sheet for "Custom…" on the quick-add bar. Validation is the pure
 * [VolumeRules]; the confirm button stays disabled until the field parses, so
 * nothing invalid can be persisted.
 */
@Composable
fun CustomVolumeSheet(
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val c = LocalSippyColors.current
    var text by remember { mutableStateOf("") }
    val value = VolumeRules.parse(text)
    val error = if (text.isBlank()) null else VolumeRules.error(text)
    val shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card)

    SheetScrim(onDismiss = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            GlassCard(shape = shape, modifier = Modifier.fillMaxWidth()) {
                Text("Custom amount", style = SippyType.title, color = c.text1)
                Text(
                    text = "Water, ${VolumeRules.MIN_ML}–${VolumeRules.MAX_ML} ml.",
                    style = SippyType.caption,
                    color = c.text2,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    label = { Text("Millilitres") },
                    suffix = { Text("ml") },
                    supportingText = { error?.let { Text(it, color = c.danger) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(4.dp))
                GlassChipRow {
                    VolumeRules.PRESETS.forEach { preset ->
                        GlassChip(
                            label = "$preset",
                            selected = value == preset,
                            onClick = { text = preset.toString() },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                GlassButton(
                    label = if (value != null) "Add ${value} ml" else "Add",
                    onClick = { value?.let(onPick) },
                    enabled = value != null,
                    prominent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                GlassButton(
                    label = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Entry editor: change one drink's volume or delete it. The volume goes back
 * through [VolumeRules], and the controller recomputes the hydrated amount, so
 * an edit can move the goal, the streak and the orb.
 */
@Composable
fun EditEntrySheet(
    entry: LogEntry,
    goalMl: Int,
    onSave: (Int, Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    var text by remember(entry.id) { mutableStateOf(entry.volumeMl.toString()) }
    var confirmDelete by remember(entry.id) { mutableStateOf(false) }
    // A3: the time is editable too, so a back-dated sip can be corrected.
    var timeMs by remember(entry.id) { mutableStateOf(entry.atEpochMs) }
    val value = VolumeRules.parse(text)
    val error = if (text.isBlank()) null else VolumeRules.error(text)
    val shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card)
    val time = Instant.ofEpochMilli(timeMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(sheetTimeFormat)
    val percent = if (goalMl > 0) {
        (entry.hydratedMl.toFloat() / goalMl * 100f).toInt()
    } else {
        0
    }

    SheetScrim(onDismiss = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            GlassCard(shape = shape, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    DrinkIcon(drinkId = drink.id, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(drink.name, style = SippyType.title, color = c.text1)
                        Text(
                            text = "$time · ${entry.hydratedMl} ml counted · $percent% of goal",
                            style = SippyType.caption,
                            color = c.text2,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    label = { Text("Volume") },
                    suffix = { Text("ml") },
                    supportingText = { error?.let { Text(it, color = c.danger) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(4.dp))
                GlassChipRow {
                    VolumeRules.PRESETS.forEach { preset ->
                        GlassChip(
                            label = "$preset",
                            selected = value == preset,
                            onClick = { text = preset.toString() },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassChip(label = "−15 min", onClick = { timeMs -= 15 * 60_000L })
                    Text(
                        text = "Time $time",
                        style = SippyType.caption,
                        color = c.text2,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    GlassChip(label = "+15 min", onClick = { timeMs += 15 * 60_000L })
                }
                Spacer(Modifier.height(12.dp))
                GlassButton(
                    label = if (value != null) "Save ${value} ml" else "Save",
                    onClick = { value?.let { onSave(it, timeMs) } },
                    enabled = value != null,
                    prominent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (confirmDelete) {
                    Text(
                        text = "Delete removes this drink from the log.",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                    GlassButton(
                        label = "Yes, delete this drink",
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    GlassButton(
                        label = "Delete",
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                GlassButton(
                    label = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
