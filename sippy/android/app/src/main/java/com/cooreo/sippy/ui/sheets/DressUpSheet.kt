package com.cooreo.sippy.ui.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.ui.components.FriendAvatar
import com.cooreo.sippy.ui.components.ItemImage
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassIconButton
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType

/**
 * Dress-up sheet (round 8, A4): a large preview that changes the instant an item
 * is tapped, the two slots, and the owned items for that friend in a grid.
 */
@Composable
fun DressUpSheet(
    friend: Friend,
    mood: Mood,
    owned: Set<String>,
    hats: Map<String, String>,
    outfits: Map<String, String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(topStart = Dimens.card, topEnd = Dimens.card)
    val worn = ShopRules.worn(hats, outfits, owned, friend.id)
    val hat = worn.firstOrNull { it.slot == ItemSlot.HAT }
    val outfit = worn.firstOrNull { it.slot == ItemSlot.OUTFIT }
    val available = ItemTable.forKind(friend.kind).filter { it.id in owned }

    SheetScrim(onDismiss = onDismiss) {
        GlassCard(shape = shape, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dress up ${friend.name}", style = SippyType.title, color = c.text1)
                    Text(
                        text = "One hat, one outfit. Tap an item to wear it, tap again to take it off.",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
                GlassIconButton(contentDescription = "Close dress up", onClick = onDismiss) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(
                            color = c.text1,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = c.text1,
                            start = Offset(size.width, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FriendAvatar(
                    friend = friend,
                    mood = mood,
                    size = 168.dp,
                    animate = true,
                    hat = hat,
                    outfit = outfit,
                )
            }
            Spacer(Modifier.height(8.dp))
            SlotRow(label = "Hat", worn = hat, onTakeOff = { hat?.let { onToggle(it.id) } })
            Spacer(Modifier.height(6.dp))
            SlotRow(label = "Outfit", worn = outfit, onTakeOff = { outfit?.let { onToggle(it.id) } })
            Spacer(Modifier.height(10.dp))
            if (available.isEmpty()) {
                Text(
                    text = "Nothing owned for ${friend.kind.short} yet. The Shop sells what exists.",
                    style = SippyType.caption,
                    color = c.text3,
                )
            } else {
                available.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            ItemCell(
                                item = item,
                                worn = worn.any { it.id == item.id },
                                onClick = { onToggle(item.id) },
                            )
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) {} }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SlotRow(label: String, worn: ShopItem?, onTakeOff: () -> Unit) {
    val c = LocalSippyColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = SippyType.emphasis, color = c.text1, modifier = Modifier.weight(1f))
        Text(
            text = worn?.name ?: "empty",
            style = SippyType.caption,
            color = if (worn == null) c.text3 else c.text2,
        )
        if (worn != null) {
            Spacer(Modifier.width(6.dp))
            GlassChip(label = "Take off", onClick = onTakeOff)
        }
    }
}

@Composable
private fun RowScope.ItemCell(item: ShopItem, worn: Boolean, onClick: () -> Unit) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.inner)
    GlassSurface(
        shape = shape,
        tint = if (worn) c.accent.copy(alpha = 0.42f) else null,
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                ItemImage(item = item, size = 44.dp)
            }
            Text(
                text = item.name,
                style = SippyType.caption,
                color = c.text1,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Visible,
            )
            Text(
                text = if (worn) "Worn" else "Tap to wear",
                style = SippyType.caption,
                color = if (worn) c.accentBright else c.text3,
            )
        }
    }
}
