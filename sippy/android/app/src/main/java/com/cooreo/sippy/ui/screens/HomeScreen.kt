package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.water.VolumeRules
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.components.FriendImage
import com.cooreo.sippy.ui.components.WaterOrb
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/**
 * Home (Today), rebuilt on the site's language: glass header chips, the water
 * orb, a quick-add bar, the day's drinks as glass cards, and the friend strip.
 * Pure in its inputs — every action is a callback, so it stays previewable and
 * has no dependency on the controller.
 */
@Composable
fun HomeScreen(
    state: StoredState,
    todayEntries: List<LogEntry>,
    hydratedMl: Int,
    goalMl: Int,
    animate: Boolean,
    ringOverride: Float?,
    bottomPadding: Dp,
    onQuickAdd: (Int) -> Unit,
    onCustomAdd: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenReminders: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
    onFriendTap: (Friend) -> Unit,
    onSeeAll: () -> Unit,
) {
    val c = LocalSippyColors.current
    val fraction = ringOverride ?: WaterMath.goalFraction(hydratedMl, goalMl)
    val remaining = WaterMath.remainingMl(hydratedMl, goalMl)
    val goalMet = WaterMath.goalMet(hydratedMl, goalMl)
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = 16.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DateChip(today)
                    Spacer(Modifier.weight(1f))
                    StreakBadge(days = state.streak.currentDays)
                    Spacer(Modifier.width(8.dp))
                    BellButton(onClick = onOpenReminders)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = greeting(state.settings.displayName),
                    style = SippyType.title,
                    color = c.text1,
                )
                Text(
                    text = when {
                        goalMet -> "Goal met — ${state.streak.currentDays}-day streak alive."
                        todayEntries.isEmpty() -> "Nothing logged yet. One sip starts it."
                        else -> "$remaining ml to go today."
                    },
                    style = SippyType.body,
                    color = c.text2,
                )
            }
        }

        item(key = "orb") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaterOrb(
                    hydratedMl = hydratedMl,
                    goalMl = goalMl,
                    fraction = fraction,
                    animate = animate,
                    diameter = 248.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        item(key = "quick-add") {
            QuickAddBar(onQuickAdd = onQuickAdd, onCustomAdd = onCustomAdd)
        }

        if (todayEntries.isEmpty()) {
            item(key = "empty") {
                EmptyState()
            }
        } else {
            item(key = "list-title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Today", style = SippyType.emphasis, color = c.text1)
                    Spacer(Modifier.weight(1f))
                    // Home keeps the three most recent; the dated log lives behind
                    // "See all" (B1).
                    GlassChip(label = "See all", onClick = onSeeAll)
                }
            }
            items(todayEntries.take(3), key = { it.id }) { entry ->
                EntryCard(entry = entry, goalMl = goalMl, onClick = { onEditEntry(entry) })
            }
        }

        item(key = "friends") {
            FriendStrip(
                state = state,
                goalMetToday = goalMet,
                hasEntriesToday = todayEntries.isNotEmpty(),
                onOpenFriends = onOpenFriends,
                onFriendTap = onFriendTap,
            )
        }
    }
}

@Composable
private fun DateChip(day: LocalDate) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.touch)
            .clip(shape)
            .semantics { contentDescription = "Today, ${day.format(dateFormat)}" },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = shape, shadow = false) {
            Text(
                text = day.format(dateFormat),
                style = SippyType.caption,
                color = c.text1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.touch)
            .clip(shape)
            .semantics {
                contentDescription = if (days == 0) "No streak yet" else "$days day streak"
            },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = shape, shadow = false) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(15.dp)) {
                    val flame = Path().apply {
                        moveTo(size.width * 0.5f, 0f)
                        cubicTo(
                            size.width * 0.96f, size.height * 0.42f,
                            size.width * 0.78f, size.height,
                            size.width * 0.5f, size.height,
                        )
                        cubicTo(
                            size.width * 0.22f, size.height,
                            size.width * 0.04f, size.height * 0.42f,
                            size.width * 0.5f, 0f,
                        )
                        close()
                    }
                    drawPath(flame, c.warn)
                    drawPath(
                        Path().apply {
                            moveTo(size.width * 0.5f, size.height * 0.42f)
                            cubicTo(
                                size.width * 0.74f, size.height * 0.66f,
                                size.width * 0.64f, size.height * 0.94f,
                                size.width * 0.5f, size.height * 0.94f,
                            )
                            cubicTo(
                                size.width * 0.36f, size.height * 0.94f,
                                size.width * 0.26f, size.height * 0.66f,
                                size.width * 0.5f, size.height * 0.42f,
                            )
                            close()
                        },
                        c.bg0.copy(alpha = 0.28f),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("$days", style = SippyType.emphasis, color = c.text1)
            }
        }
    }
}

@Composable
private fun BellButton(onClick: () -> Unit) {
    val c = LocalSippyColors.current
    Box(
        modifier = Modifier
            .size(Dimens.touch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Reminders" },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = CircleShape, shadow = false) {
            Canvas(
                modifier = Modifier
                    .size(22.dp)
                    .padding(2.dp)
                    .clearAndSetSemantics { },
            ) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
                val body = Path().apply {
                    moveTo(w * 0.20f, h * 0.68f)
                    cubicTo(w * 0.30f, h * 0.58f, w * 0.28f, h * 0.34f, w * 0.50f, h * 0.24f)
                    cubicTo(w * 0.72f, h * 0.34f, w * 0.70f, h * 0.58f, w * 0.80f, h * 0.68f)
                    close()
                }
                drawPath(body, c.text1, style = stroke)
                drawPath(
                    Path().apply {
                        moveTo(w * 0.14f, h * 0.70f)
                        lineTo(w * 0.86f, h * 0.70f)
                    },
                    c.text1,
                    style = stroke,
                )
                drawPath(
                    Path().apply {
                        moveTo(w * 0.42f, h * 0.82f)
                        cubicTo(w * 0.45f, h * 0.92f, w * 0.55f, h * 0.92f, w * 0.58f, h * 0.82f)
                    },
                    c.text1,
                    style = stroke,
                )
            }
        }
    }
}

@Composable
private fun QuickAddBar(onQuickAdd: (Int) -> Unit, onCustomAdd: () -> Unit) {
    val c = LocalSippyColors.current
    GlassCard(contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VolumeRules.PRESETS.forEach { volume ->
                Box(Modifier.weight(1f)) {
                    GlassChip(
                        label = "+$volume",
                        onClick = { onQuickAdd(volume) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Add $volume millilitres of water" },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassChip(
            label = "Custom…",
            onClick = onCustomAdd,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Add a custom volume" },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Quick adds log water.",
            style = SippyType.caption,
            color = c.text3,
        )
    }
}

@Composable
private fun EmptyState() {
    val c = LocalSippyColors.current
    GlassSurface(shape = RoundedCornerShape(Dimens.card)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FriendImage(
                kind = com.cooreo.sippy.model.FriendKind.DIPPY,
                mood = Mood.SLEEPY,
                growthStreak = 0,
                size = 116,
                animate = false,
                modifier = Modifier.size(116.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text("Log your first sip", style = SippyType.emphasis, color = c.text1)
            Text(
                text = "Dippy is asleep until something counts.",
                style = SippyType.caption,
                color = c.text2,
            )
        }
    }
}

@Composable
private fun EntryCard(entry: LogEntry, goalMl: Int, onClick: () -> Unit) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    val shape = RoundedCornerShape(Dimens.inner)
    val percent = if (goalMl > 0) {
        (entry.hydratedMl.toFloat() / goalMl * 100f).roundToInt()
    } else {
        0
    }
    val time = Instant.ofEpochMilli(entry.atEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormat)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "${drink.name}, ${entry.volumeMl} millilitres at $time, " +
                    "$percent percent of the goal. Double tap to edit."
            },
    ) {
        GlassSurface(shape = shape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrinkIcon(
                    drinkId = entry.drinkId,
                    size = 42.dp,
                    label = null,
                )
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
                        text = "${entry.volumeMl} ml · $time",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                    Text(
                        text = "${entry.hydratedMl} ml counted",
                        style = SippyType.caption,
                        color = c.text3,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$percent%",
                    style = SippyType.emphasis,
                    color = c.accentBright,
                )
            }
        }
    }
}

@Composable
private fun FriendStrip(
    state: StoredState,
    goalMetToday: Boolean,
    hasEntriesToday: Boolean,
    onOpenFriends: () -> Unit,
    onFriendTap: (Friend) -> Unit,
) {
    val c = LocalSippyColors.current
    GlassSurface(shape = RoundedCornerShape(Dimens.card)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Five friends at 360dp would overflow: scroll instead of clipping.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.friends.forEach { friend ->
                    val mood = moodFor(friend, state, goalMetToday, hasEntriesToday)
                    Column(
                        modifier = Modifier
                            .defaultMinSize(minWidth = Dimens.touch, minHeight = Dimens.touch)
                            .clip(RoundedCornerShape(Dimens.inner))
                            .clickable(role = Role.Button, onClick = { onFriendTap(friend) })
                            .semantics {
                                contentDescription = "${friend.name}, ${mood.name.lowercase()}"
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FriendImage(
                            kind = friend.kind,
                            mood = mood,
                            growthStreak = friend.grownAtStreak,
                            size = 52,
                            animate = true,
                            modifier = Modifier.size(52.dp),
                        )
                        Text(friend.name, style = SippyType.caption, color = c.text2)
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Friends ›",
                style = SippyType.caption,
                color = c.text3,
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.touch)
                    .clip(RoundedCornerShape(Dimens.pill))
                    .clickable(role = Role.Button, onClick = onOpenFriends)
                    .semantics { contentDescription = "Open friends" }
                    .padding(horizontal = 10.dp, vertical = 14.dp),
            )
        }
    }
}

/** Same derivation the friends screen uses: mood is never stored. */
fun moodFor(
    friend: Friend,
    state: StoredState,
    goalMetToday: Boolean,
    hasEntriesToday: Boolean,
): Mood = MoodRules.forDay(
    goalMetToday = goalMetToday,
    hasEntriesToday = hasEntriesToday,
    currentStreakDays = state.streak.currentDays,
    yesterdayMissedAfterStreak = friend.grownAtStreak >= 3 &&
        state.streak.currentDays < friend.grownAtStreak,
)

/** One line per mood, shown when a friend in the strip is tapped. */
fun moodLine(name: String, mood: Mood): String = when (mood) {
    Mood.DANCING -> "$name is dancing — goal met and the streak is holding."
    Mood.HAPPY -> "$name is happy with today's sips."
    Mood.SLEEPY -> "$name is dozing. Nothing logged yet today."
    Mood.SAD -> "$name misses the streak. A glass would fix that."
}

/** "Good morning", plus the name only when the user has set one. */
fun greeting(name: String, hour: Int = LocalTime.now().hour): String {
    val part = when {
        hour < 5 -> "Still up"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val clean = name.trim()
    return if (clean.isEmpty()) part else "$part, $clean"
}
