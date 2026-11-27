package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.log.DayGroup
import com.cooreo.sippy.domain.log.LogGroups
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.glass.GlassCard
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
 * The dated log (B1): every entry ever recorded, grouped by day with Today /
 * Yesterday / date headers, newest first. Home shows the last three of today and
 * links here for the rest.
 */
@Composable
fun HistoryScreen(
    state: StoredState,
    bottomPadding: Dp,
    onBack: () -> Unit,
) {
    val c = LocalSippyColors.current
    val today = Dates.todayEpochDay()
    val groups = remember(state.log, today) { LogGroups.byDay(state.log, today) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = 16.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("History", style = SippyType.title, color = c.text1)
                    Text(
                        text = if (groups.isEmpty()) {
                            "Nothing logged yet."
                        } else {
                            "${state.log.size} drinks across ${groups.size} " +
                                if (groups.size == 1) "day." else "days."
                        },
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
                GlassChip(label = "Back", onClick = onBack)
            }
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                GlassCard {
                    Text("Log your first sip", style = SippyType.emphasis, color = c.text1)
                    Text(
                        text = "Every drink you log lands here, day by day.",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
            }
        }

        groups.forEach { group ->
            item(key = "day-${group.dayEpoch}") {
                DayHeader(group)
            }
            items(group.entries.size, key = { index -> "e-${group.entries[index].id}" }) { index ->
                Box(Modifier.animateItem()) {
                    HistoryRow(group.entries[index], goalMl = state.settings.goalMl)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    val c = LocalSippyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(group.label, style = SippyType.emphasis, color = c.text1)
        Spacer(Modifier.weight(1f))
        Text(
            text = "${group.volumeMl} ml · ${group.countedMl} counted",
            style = SippyType.caption,
            color = c.text2,
        )
    }
}

@Composable
private fun HistoryRow(entry: com.cooreo.sippy.model.LogEntry, goalMl: Int) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    val time = Instant.ofEpochMilli(entry.atEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(historyTimeFormat)
    val shape = RoundedCornerShape(Dimens.inner)

    GlassSurface(shape = shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrinkIcon(drinkId = entry.drinkId, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = drink.name,
                    style = SippyType.emphasis,
                    color = c.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$time · ${entry.volumeMl} ml · ${entry.hydratedMl} ml counted",
                    style = SippyType.caption,
                    color = c.text2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (goalMl > 0) {
                    "${(entry.hydratedMl * 100f / goalMl).toInt()}%"
                } else {
                    "—"
                },
                style = SippyType.caption,
                color = c.accentBright,
            )
        }
    }
}
