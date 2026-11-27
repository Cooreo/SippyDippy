package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.PointsEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.ItemImage
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType

/**
 * Points, the price table and the ledger (round 7, B4). No real money and no
 * ads anywhere: points come only from counted hydration.
 */
@Composable
fun ShopScreen(
    state: StoredState,
    onBuy: (String) -> Unit,
    onEquip: (String, String) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val c = LocalSippyColors.current
    val carry = state.pointsCarryMl.coerceIn(0, PointsRules.STEP_ML - 1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Shop", style = SippyType.display, color = c.text1)
                        Text(
                            text = "One point per 250 ml you count. Nothing here costs money.",
                            style = SippyType.caption,
                            color = c.text2,
                        )
                    }
                    GlassChip(label = "Back", onClick = onBack)
                }
            }
        }

        item {
            GlassCard {
                Column {
                    Text("Your points", style = SippyType.label, color = c.text3)
                    Text("${state.points}", style = SippyType.value, color = c.text1)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$carry ml carried toward the next point " +
                            "(${PointsRules.millilitresToNextPoint(carry)} ml to go).",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
            }
        }

        ItemTable.groups().forEach { (kind, items) ->
            item(key = "group-${kind.name}") {
                val friend = state.friends.firstOrNull { it.kind == kind }
                GlassCard {
                    Column {
                        Text(kind.short, style = SippyType.title, color = c.text1)
                        if (friend == null) {
                            Text(
                                text = "Add a ${kind.short} in Friends to wear this.",
                                style = SippyType.caption,
                                color = c.text3,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        items.forEach { item ->
                            ItemRow(
                                item = item,
                                owned = item.id in state.ownedItems,
                                equipped = friend != null && state.equipped[friend.id] == item.id,
                                canWear = friend != null,
                                price = item.price,
                                balance = state.points,
                                onBuy = { onBuy(item.id) },
                                onEquip = { friend?.let { onEquip(it.id, item.id) } },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        if (state.ledger.isNotEmpty()) {
            item {
                GlassCard {
                    Column {
                        Text("Recent points", style = SippyType.title, color = c.text1)
                        Spacer(Modifier.height(6.dp))
                        state.ledger.takeLast(6).reversed().forEach { entry ->
                            LedgerRow(entry)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Points are earned by logging drinks and are never bought. " +
                    "Owning a cosmetic keeps it, even if you remove that friend.",
                style = SippyType.caption,
                color = c.text3,
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: ShopItem,
    owned: Boolean,
    equipped: Boolean,
    canWear: Boolean,
    price: Int,
    balance: Int,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
) {
    val c = LocalSippyColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ItemImage(item = item, size = 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.name, style = SippyType.emphasis, color = c.text1)
            Text(
                text = if (owned) "Owned" else "Proposed price: $price points",
                style = SippyType.caption,
                color = c.text3,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            !owned -> GlassChip(
                label = if (balance >= price) "Buy $price" else "Need ${price - balance}",
                onClick = onBuy,
            )
            !canWear -> Text("Owned", style = SippyType.caption, color = c.text3)
            equipped -> GlassChip(label = "Worn", selected = true, onClick = onEquip)
            else -> GlassChip(label = "Wear", onClick = onEquip)
        }
    }
}

@Composable
private fun LedgerRow(entry: PointsEntry) {
    val c = LocalSippyColors.current
    val sign = if (entry.delta >= 0) "+" else "−"
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(entry.reason, style = SippyType.caption, color = c.text2, modifier = Modifier.weight(1f))
        Text(
            text = "$sign${kotlin.math.abs(entry.delta)}",
            style = SippyType.caption,
            color = if (entry.delta >= 0) c.text1 else c.text3,
        )
        Box(Modifier.width(10.dp))
        Text("${entry.balanceAfter}", style = SippyType.caption, color = c.text3)
    }
}
