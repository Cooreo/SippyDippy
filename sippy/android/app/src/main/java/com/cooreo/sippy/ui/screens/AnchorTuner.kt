package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.ui.components.FriendAvatar
import com.cooreo.sippy.ui.friends.Anchors
import com.cooreo.sippy.ui.friends.OverlayAnchor
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassChipRow
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import java.util.Locale

/**
 * Developer options → "Anchor tuner" (round 8, A4). Pick a species and a slot,
 * nudge the overlay on a large preview, then read the four numbers off the screen
 * and send them back — they are pasted into [Anchors], which is the only place an
 * overlay position lives.
 */
@Composable
fun AnchorTunerCard() {
    val c = LocalSippyColors.current
    var kindIndex by remember { mutableIntStateOf(0) }
    var slotIndex by remember { mutableIntStateOf(0) }
    val kind = FriendKind.entries[kindIndex]
    val slot = ItemSlot.entries[slotIndex]
    val base = Anchors.forSpecies(kind, slot)

    var dx by remember(kind, slot) { mutableFloatStateOf(base.dx) }
    var dy by remember(kind, slot) { mutableFloatStateOf(base.dy) }
    var scale by remember(kind, slot) { mutableFloatStateOf(base.scale) }
    var rotation by remember(kind, slot) { mutableFloatStateOf(base.rotationDeg) }

    val item = ItemTable.items.firstOrNull { it.kind == kind && it.slot == slot }
    val preview = Friend(
        id = "tuner",
        kind = kind,
        name = kind.short,
    )
    val override = OverlayAnchor(dx = dx, dy = dy, scale = scale, rotationDeg = rotation)

    GlassCard {
        Column {
            Text("Anchor tuner", style = SippyType.emphasis, color = c.text1)
            Text(
                text = "Pick a species and slot, nudge the item, then send me the numbers " +
                    "printed below. They go straight into Anchors.kt.",
                style = SippyType.tab,
                color = c.text3,
            )
            Spacer(Modifier.height(8.dp))
            GlassChipRow {
                GlassChip(
                    label = kind.short,
                    onClick = { kindIndex = (kindIndex + 1) % FriendKind.entries.size },
                )
                GlassChip(
                    label = slot.name.lowercase(Locale.US),
                    onClick = { slotIndex = (slotIndex + 1) % ItemSlot.entries.size },
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FriendAvatar(
                    friend = preview,
                    mood = Mood.HAPPY,
                    size = 200.dp,
                    animate = false,
                    hat = if (slot == ItemSlot.HAT) item else null,
                    outfit = if (slot == ItemSlot.OUTFIT) item else null,
                    anchorOverride = override,
                    tuneSlot = slot,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "dx=%.3f  dy=%.3f  scale=%.3f  rot=%.1f",
                    dx,
                    dy,
                    scale,
                    rotation,
                ),
                style = SippyType.secondary,
                color = c.accentBright,
            )
            Text(
                text = if (item == null) {
                    "No ${slot.name.lowercase(Locale.US)} art exists for ${kind.short} yet — the numbers still apply."
                } else {
                    "Previewing ${item.name} on ${kind.short}"
                },
                style = SippyType.tab,
                color = c.text3,
            )
            SliderRow("dx", dx, -0.5f, 0.5f) { dx = it }
            SliderRow("dy", dy, -0.5f, 0.5f) { dy = it }
            SliderRow("scale", scale, 0.15f, 0.95f) { scale = it }
            SliderRow("rot", rotation, -45f, 45f) { rotation = it }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    from: Float,
    to: Float,
    onChange: (Float) -> Unit,
) {
    val c = LocalSippyColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = SippyType.tab,
            color = c.text3,
            modifier = Modifier.fillMaxWidth(0.16f),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = from..to,
            modifier = Modifier.fillMaxWidth(0.84f),
        )
    }
}
