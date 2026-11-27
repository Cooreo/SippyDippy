package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FriendImage
import com.cooreo.sippy.ui.components.WaterRing
import com.cooreo.sippy.ui.glass.GlassButton
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TodayScreen(
    state: StoredState,
    controller: SippyController,
    todayEntries: List<LogEntry>,
    hydratedMl: Int,
    goalMl: Int,
    animate: Boolean,
    ringOverride: Float?,
    onAddDrink: () -> Unit,
    onOpenFriends: () -> Unit,
) {
    val c = LocalSippyColors.current
    val fraction = ringOverride ?: WaterMath.goalFraction(hydratedMl, goalMl)
    val remaining = WaterMath.remainingMl(hydratedMl, goalMl)
    val goalMet = WaterMath.goalMet(hydratedMl, goalMl)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = greeting(),
                    color = c.text1,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (goalMet) {
                        "Goal met — ${state.streak.currentDays}-day streak alive."
                    } else {
                        "$remaining ml to go today."
                    },
                    color = c.text2,
                    fontSize = 14.sp,
                )
            }
        }

        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaterRing(
                    hydratedMl = hydratedMl,
                    goalMl = goalMl,
                    fraction = fraction,
                    animate = animate,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(label = "Streak", value = "${state.streak.currentDays}d", modifier = Modifier.weight(1f))
                StatPill(label = "Best", value = "${state.streak.bestDays}d", modifier = Modifier.weight(1f))
                StatPill(
                    label = "Sips",
                    value = todayEntries.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            GlassCard {
                Text("Add water", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(250, 330, 500).forEach { volume ->
                        Box(Modifier.weight(1f)) {
                            GlassChip(
                                label = "+$volume",
                                onClick = {
                                    controller.addDrink(DrinkTable.byId("water"), volume)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    label = "Pick a drink",
                    onClick = onAddDrink,
                    prominent = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            GlassCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.inner))
                        .clickable(onClick = onOpenFriends),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.friends.forEach { friend ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FriendImage(
                                kind = friend.kind,
                                mood = controller.moodOverride ?: moodFor(friend.grownAtStreak, state.streak.currentDays, goalMet),
                                growthStreak = friend.grownAtStreak,
                                size = 56,
                                animate = animate,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(friend.name, color = c.text2, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Friends ›", color = c.text3, fontSize = 12.sp)
                }
            }
        }

        if (todayEntries.isEmpty()) {
            item {
                Text(
                    text = "Nothing logged yet today. Dippy is waiting.",
                    color = c.text3,
                    fontSize = 13.sp,
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Today", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Undo last",
                        color = c.accentBright,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .defaultMinSize(minHeight = Dimens.touch)
                            .clip(RoundedCornerShape(Dimens.pill))
                            .clickable(onClick = { controller.undoLast() })
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
            }
            items(todayEntries.reversed(), key = { it.id }) { entry ->
                EntryRow(entry = entry, onDelete = { controller.deleteEntry(entry.id) })
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LocalSippyColors.current
    GlassCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(value, color = c.text1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = c.text3, fontSize = 11.sp)
    }
}

@Composable
private fun EntryRow(entry: LogEntry, onDelete: () -> Unit) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(drink.name, color = c.text1, fontSize = 14.sp)
                Text(
                    text = "${entry.volumeMl} ml · ${entry.hydratedMl} ml counted · " +
                        LocalTime.ofEpochMilli(entry.atEpochMs).format(timeFormat),
                    color = c.text3,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "✕",
                color = c.text3,
                fontSize = 16.sp,
                modifier = Modifier
                    .defaultMinSize(minWidth = Dimens.touch, minHeight = Dimens.touch)
                    .clip(RoundedCornerShape(Dimens.pill))
                    .clickable(onClick = onDelete)
                    .padding(14.dp),
            )
        }
    }
}

/** Friend mood: derived from today's progress and the friend's own growth. */
private fun moodFor(grownAtStreak: Int, currentStreak: Int, goalMetToday: Boolean): Mood =
    MoodRules.forDay(
        goalMetToday = goalMetToday,
        hasEntriesToday = true,
        currentStreakDays = currentStreak,
        yesterdayMissedAfterStreak = grownAtStreak >= 3 && currentStreak < grownAtStreak,
    )

private fun greeting(hour: Int = LocalTime.now().hour): String = when {
    hour < 5 -> "Still up?"
    hour < 12 -> "Good morning"
    hour < 18 -> "Good afternoon"
    else -> "Good evening"
}
