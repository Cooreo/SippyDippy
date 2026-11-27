package com.cooreo.sippy.ui.sheets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.drinks.Drink
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.updates.UpdateManifest
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooreo.sippy.update.UpdateManager
import kotlinx.coroutines.flow.StateFlow

/** Scrim + bottom card. Used by every sheet so they all behave the same. */
@Composable
private fun SheetScrim(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(onClick = onDismiss),
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            content()
        }
    }
}

@Composable
fun AddDrinkSheet(
    onDismiss: () -> Unit,
    onPick: (Drink, Int) -> Unit,
) {
    val c = LocalSippyColors.current
    var selected by remember { mutableStateOf(DrinkTable.drinks.first()) }
    var custom by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card)

    SheetScrim(onDismiss = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                contentPadding = PaddingValues(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Log a drink", color = c.text1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Close",
                        color = c.text3,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${selected.name} · counts ${(selected.hydrationFactor * 100).toInt()}%" +
                        if (selected.sugary) " · sugary" else "",
                    color = c.text3,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(DrinkTable.drinks, key = { it.id }) { drink ->
                        GlassChip(
                            label = "${drink.name} · ${drink.defaultVolumeMl} ml",
                            selected = drink.id == selected.id,
                            onClick = { selected = drink },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(150, 200, 250, 330, 500).forEach { volume ->
                        Box(Modifier.weight(1f)) {
                            GlassChip(
                                label = "$volume",
                                onClick = { onPick(selected, volume) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { raw -> custom = raw.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    label = { Text("Custom amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    label = "Add ${custom.ifEmpty { selected.defaultVolumeMl.toString() }} ml of ${selected.name}",
                    prominent = true,
                    onClick = {
                        val volume = custom.toIntOrNull() ?: selected.defaultVolumeMl
                        onPick(selected, volume.coerceIn(1, Settings.MAX_GOAL_ML))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun UpdateSheet(
    manifest: UpdateManifest,
    progress: StateFlow<UpdateManager.Progress>,
    installing: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalSippyColors.current
    val p by progress.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card)
    SheetScrim(onDismiss = onDismiss) {
        GlassCard(shape = shape, modifier = Modifier.fillMaxWidth()) {
            Text("Sippy ${manifest.versionName} is available", color = c.text1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (manifest.notes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                manifest.notes.take(5).forEach { note ->
                    Text("• $note", color = c.text2, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (p.downloading) {
                val fraction = if (p.total > 0) (p.bytes.toFloat() / p.total) else 0f
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${p.bytes / 1024} / ${if (p.total > 0) p.total / 1024 else 0} kB",
                    color = c.text3,
                    fontSize = 11.sp,
                )
            } else {
                Text(
                    text = "Android will ask you to confirm the install, and may ask you to allow " +
                        "installs from Sippy the first time.",
                    color = c.text3,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    label = if (installing) "Working…" else "Download & install",
                    prominent = true,
                    onClick = { if (!installing && !p.downloading) onInstall() },
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    label = "Later",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Small alias so the sheet does not depend on the update package. */
typealias UpdateManagerProgress = com.cooreo.sippy.update.UpdateManager.Progress

