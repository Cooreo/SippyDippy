package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.DrinkCategory
import com.cooreo.sippy.ui.theme.DrinkTints
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.log.DayGroup
import com.cooreo.sippy.domain.log.LogGroups
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.components.ShareBar
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import com.cooreo.sippy.util.Dates
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val historyTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The full dated log (round 8, A3): every entry ever recorded, under sticky day
 * headers, newest first. Tap a row to edit its volume or time, swipe it away to
 * delete, and the caller offers Undo on the toast.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: StoredState,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
    onDeleteEntry: (LogEntry) -> Unit,
) {
    val c = LocalSippyColors.current
    val today = Dates.todayEpochDay()
    val goal = state.settings.goalMl
    val groups = remember(state.log, today, goal) {
        LogGroups.byDay(state.log, today, goalMl = goal)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = 8.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("History", style = SippyType.title, color = c.text1)
                    Text(
                        text = "${state.log.size} ${if (state.log.size == 1) "entry" else "entries"} · " +
                            "${groups.size} ${if (groups.size == 1) "day" else "days"}",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
                GlassChip(label = "Back", onClick = onBack)
            }
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "Nothing logged yet. Your first sip will appear here.",
                    style = SippyType.body,
                    color = c.text2,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        groups.forEach { group ->
            stickyHeader(key = "day-${group.dayEpoch}") {
                DayHeader(group)
            }
            items(group.entries.size, key = { index -> group.entries[index].id }) { index ->
                val entry = group.entries[index]
                SwipeableRow(
                    entry = entry,
                    goalMl = goal,
                    onClick = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

/** Sticky day header: label on the left, counted millilitres and goal tick right. */
@Composable
private fun DayHeader(group: DayGroup) {
    val c = LocalSippyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Opaque enough that rows scrolling underneath cannot be read through
            // the header while it is pinned.
            .background(c.bg0.copy(alpha = 0.88f))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.label,
            style = SippyType.label,
            color = c.text2,
            modifier = Modifier.weight(1f),
        )
        if (group.goalMet) {
            Text("✓ ", style = SippyType.caption, color = c.ok)
        }
        Text(
            text = "${group.countedMl} ml",
            style = SippyType.caption,
            color = if (group.goalMet) c.ok else c.text3,
        )
    }
}

@Composable
private fun SwipeableRow(
    entry: LogEntry,
    goalMl: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalSippyColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Dimens.inner))
                    .background(c.danger.copy(alpha = 0.25f))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", style = SippyType.emphasis, color = c.text1)
            }
        },
    ) {
        HistoryRow(entry = entry, goalMl = goalMl, onClick = onClick)
    }
}

@Composable
private fun HistoryRow(entry: LogEntry, goalMl: Int, onClick: () -> Unit) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    val time = Instant.ofEpochMilli(entry.atEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(historyTimeFormat)
    val percent = if (goalMl > 0) entry.hydratedMl * 100 / goalMl else 0

    GlassSurface(
        shape = RoundedCornerShape(Dimens.inner),
        tint = DrinkTints.card(DrinkCategory.of(entry.drinkId)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.inner))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, style = SippyType.caption, color = c.text3, modifier = Modifier.width(46.dp))
            DrinkIcon(drinkId = drink.id, size = 28.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(drink.name, style = SippyType.emphasis, color = c.text1)
                Text(
                    text = "${entry.volumeMl} ml · ${entry.hydratedMl} counted · $percent% of goal",
                    style = SippyType.caption,
                    color = c.text2,
                )
                Spacer(Modifier.height(6.dp))
                ShareBar(
                    fraction = if (goalMl > 0) entry.hydratedMl.toFloat() / goalMl else 0f,
                    color = DrinkTints.of(DrinkCategory.of(entry.drinkId)),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Edit", style = SippyType.caption, color = c.text3)
        }
    }
}
