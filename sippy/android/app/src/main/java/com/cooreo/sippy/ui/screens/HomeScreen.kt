package com.cooreo.sippy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.DrinkCategory
import com.cooreo.sippy.ui.theme.DrinkTints
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.layout.OrbLayout
import com.cooreo.sippy.domain.friends.PinRules
import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.domain.water.VolumeRules
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.components.FriendAvatar
import com.cooreo.sippy.ui.components.WaterOrb
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Home, rebuilt top to bottom for round 8 (A5):
 *
 *   status bar // slim header (greeting, streak, points)
 *   orb with the pinned friends in its upper corners, tap for a mood bubble
 *   three equal quick-add pills
 *   one card: "Today" + "See all", the last three rows, then the friend strip
 *
 * The layout follows the round-8 sketch; the sketch itself never reached the
 * sandbox, so the exact spacing is judged on the phone, not from this comment.
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
    onOpenFriends: () -> Unit,
    onOpenShop: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
    onFriendTap: (Friend) -> Unit,
    onSeeAll: () -> Unit,
) {
    val c = LocalSippyColors.current
    val fraction = ringOverride ?: WaterMath.goalFraction(hydratedMl, goalMl)
    val remaining = WaterMath.remainingMl(hydratedMl, goalMl)
    val goalMet = WaterMath.goalMet(hydratedMl, goalMl)
    val hasEntries = todayEntries.isNotEmpty()
    val pins = PinRules.slots(state.friends).filterNotNull()
    val host = state.friends.firstOrNull { it.kind == FriendKind.DIPPY } ?: state.friends.firstOrNull()
    val hostMood = host?.let { moodFor(it, state, goalMet, hasEntries) } ?: Mood.SLEEPY
    var bubble by remember { mutableStateOf(false) }
    LaunchedEffect(bubble) {
        if (bubble) {
            delay(4200)
            bubble = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = 12.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Header(
                title = greeting(state.settings.displayName),
                subline = when {
                    goalMet -> "Goal reached · ${hydratedMl} ml today"
                    hasEntries -> "$remaining ml to go today"
                    else -> "Nothing logged yet. One sip starts it."
                },
                streakDays = state.streak.currentDays,
                points = state.points,
                onOpenShop = onOpenShop,
            )
        }

        item(key = "orb") {
            OrbBlock(
                state = state,
                pins = pins,
                hydratedMl = hydratedMl,
                goalMl = goalMl,
                fraction = fraction,
                goalMet = goalMet,
                hasEntries = hasEntries,
                animate = animate,
                hostName = host?.name ?: "Dippy",
                hostMood = hostMood,
                bubble = bubble,
                onOrbTap = { bubble = !bubble },
                onFriendTap = onFriendTap,
            )
        }

        item(key = "pills") {
            QuickPills(onQuickAdd = onQuickAdd)
        }

        item(key = "today") {
            TodayCard(
                state = state,
                entries = todayEntries,
                goalMl = goalMl,
                hasEntries = hasEntries,
                goalMet = goalMet,
                animate = animate,
                onEditEntry = onEditEntry,
                onFriendTap = onFriendTap,
                onOpenFriends = onOpenFriends,
                onSeeAll = onSeeAll,
            )
        }
    }
}

/** Greeting on the left, two chips on the right. No bell — that lives in Settings. */
@Composable
private fun Header(
    title: String,
    subline: String,
    streakDays: Int,
    points: Int,
    onOpenShop: () -> Unit,
) {
    val c = LocalSippyColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = SippyType.title,
                color = c.text1,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            StreakChip(days = streakDays)
            Spacer(Modifier.width(6.dp))
            GlassChip(
                label = "$points pts",
                onClick = onOpenShop,
                modifier = Modifier.semantics { contentDescription = "$points points, open the shop" },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(subline, style = SippyType.caption, color = c.text2)
    }
}

@Composable
private fun StreakChip(days: Int) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.pill)
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = Dimens.touch)
            .clip(shape)
            .semantics {
                contentDescription = if (days == 0) "No streak yet" else "$days day streak"
            },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = shape, shadow = false) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(14.dp)) {
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
                }
                Spacer(Modifier.width(5.dp))
                Text("$days", style = SippyType.caption, color = c.text1)
            }
        }
    }
}

/**
 * The orb, centred on its own, with the pinned friends in its upper corners.
 * Overlaying instead of putting them in a row is what keeps the number clear:
 * the orb grows to its full 248dp and the pins never sit beside the digits.
 */
@Composable
private fun OrbBlock(
    state: StoredState,
    pins: List<Friend>,
    hydratedMl: Int,
    goalMl: Int,
    fraction: Float,
    goalMet: Boolean,
    hasEntries: Boolean,
    animate: Boolean,
    hostName: String,
    hostMood: Mood,
    bubble: Boolean,
    onOrbTap: () -> Unit,
    onFriendTap: (Friend) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 360dp phones get 320dp of content: the orb drops to 72% of that so the
        // pins, the number and the pills all still fit without scrolling apart.
        val diameter = if (maxWidth.value < 330f) {
            maxWidth.value * 0.72f
        } else {
            minOf(OrbLayout.MAX_DIAMETER_DP, maxWidth.value * 0.86f)
        }.dp

        Column {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WaterOrb(
                    hydratedMl = hydratedMl,
                    goalMl = goalMl,
                    fraction = fraction,
                    animate = animate,
                    diameter = diameter,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.pill))
                        .clickable(onClick = onOrbTap)
                        .semantics {
                            contentDescription = if (goalMet) {
                                "Goal reached, $hydratedMl millilitres. Tap for a mood."
                            } else {
                                "$hydratedMl of $goalMl millilitres. Tap for a mood."
                            }
                        },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    PinnedChip(
                        friend = pins.getOrNull(0),
                        state = state,
                        goalMetToday = goalMet,
                        hasEntriesToday = hasEntries,
                        animate = animate,
                        onTap = onFriendTap,
                    )
                    PinnedChip(
                        friend = pins.getOrNull(1),
                        state = state,
                        goalMetToday = goalMet,
                        hasEntriesToday = hasEntries,
                        animate = animate,
                        onTap = onFriendTap,
                    )
                }
            }
            AnimatedVisibility(visible = bubble) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GlassSurface(shape = RoundedCornerShape(Dimens.inner), shadow = false) {
                        Text(
                            text = moodLine(hostName, hostMood),
                            style = SippyType.caption,
                            color = LocalSippyColors.current.text1,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One pinned friend in an orb corner. Bobs by translation, never by layout. */
@Composable
private fun PinnedChip(
    friend: Friend?,
    state: StoredState,
    goalMetToday: Boolean,
    hasEntriesToday: Boolean,
    animate: Boolean,
    onTap: (Friend) -> Unit,
) {
    val c = LocalSippyColors.current
    if (friend == null) {
        Spacer(Modifier.width(56.dp))
        return
    }
    val phase = if (animate) {
        rememberInfiniteTransition(label = "friendBob").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "friendBobPhase",
        ).value
    } else {
        0f
    }
    val mood = moodFor(friend, state, goalMetToday, hasEntriesToday)
    val worn = ShopRules.worn(state.equipped, state.equippedOutfits, state.ownedItems, friend.id)

    Column(
        modifier = Modifier
            .width(56.dp)
            .defaultMinSize(minHeight = Dimens.touch)
            .clip(RoundedCornerShape(Dimens.inner))
            .clickable { onTap(friend) }
            .semantics { contentDescription = "${friend.name} is pinned; ${mood.name.lowercase()}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FriendAvatar(
            friend = friend,
            mood = mood,
            size = 44.dp,
            animate = animate,
            hat = worn.firstOrNull { it.slot == ItemSlot.HAT },
            outfit = worn.firstOrNull { it.slot == ItemSlot.OUTFIT },
            modifier = Modifier.graphicsLayer { translationY = phase * 4.dp.toPx() },
        )
        Text(
            text = friend.name,
            style = SippyType.caption,
            color = c.text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Three equal pills. The glass icon fills in proportion to what the pill logs. */
@Composable
private fun QuickPills(onQuickAdd: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        VolumeRules.PRESETS.forEach { volume ->
            Box(Modifier.weight(1f)) {
                PillsButton(volume = volume, onClick = { onQuickAdd(volume) })
            }
        }
    }
}

@Composable
private fun PillsButton(volume: Int, onClick: () -> Unit) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.inner)
    val fill = (volume.toFloat() / VolumeRules.PRESETS.max().toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Add $volume millilitres of water" },
    ) {
        GlassSurface(shape = shape, modifier = Modifier.matchParentSize(), shadow = false) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                FillIcon(fraction = fill, color = c.accentBright)
                Spacer(Modifier.width(6.dp))
                Text("+$volume", style = SippyType.emphasis, color = c.text1)
            }
        }
    }
}

/** A small glass whose fill matches the pill (250 → half, 500 → full). */
@Composable
private fun FillIcon(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(width = 14.dp, height = 18.dp)) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.82f, h)
            lineTo(w * 0.18f, h)
            close()
        }
        drawPath(body, color.copy(alpha = 0.18f))
        val top = h * (1f - fraction)
        drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w * 0.82f, h)
                lineTo(w * 0.18f, h)
                close()
                moveTo(w * 0.10f, top)
                lineTo(w * 0.90f, top)
                lineTo(w * 0.82f, h)
                lineTo(w * 0.18f, h)
                close()
            },
            color,
            style = Stroke(width = 1.4.dp.toPx()),
        )
        drawRect(
            color = color,
            topLeft = Offset(w * 0.20f, top),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h - top - 1.5.dp.toPx()),
        )
    }
}

/**
 * The one big card: "Today" and "See all" on top, the last three rows, then the
 * friends that are not pinned on the orb (up to four).
 */
@Composable
private fun TodayCard(
    state: StoredState,
    entries: List<LogEntry>,
    goalMl: Int,
    hasEntries: Boolean,
    goalMet: Boolean,
    animate: Boolean,
    onEditEntry: (LogEntry) -> Unit,
    onFriendTap: (Friend) -> Unit,
    onOpenFriends: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val c = LocalSippyColors.current
    val loose = state.friends.filterNot { it.pinned }.take(4)
    // B3: the card takes its tint from the last drink in it.
    val todayTint = entries.maxByOrNull { it.atEpochMs }
        ?.let { DrinkTints.card(DrinkCategory.of(it.drinkId)) }
    GlassCard(contentPadding = PaddingValues(16.dp), tint = todayTint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = SippyType.emphasis, color = c.text1)
            Spacer(Modifier.weight(1f))
            GlassChip(label = "See all", onClick = onSeeAll)
        }
        Spacer(Modifier.height(4.dp))
        if (!hasEntries) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FriendAvatar(
                    friend = Friend(id = "empty", kind = FriendKind.DIPPY, name = "Dippy"),
                    mood = Mood.SLEEPY,
                    size = 104.dp,
                    animate = false,
                )
                Spacer(Modifier.height(8.dp))
                Text("Log your first sip", style = SippyType.emphasis, color = c.text1)
                Text(
                    text = "Dippy is asleep until something counts.",
                    style = SippyType.caption,
                    color = c.text2,
                )
            }
        } else {
            // Newest first: today's last three, no more.
            entries.sortedByDescending { it.atEpochMs }.take(3).forEach { entry ->
                LogRow(entry = entry, goalMl = goalMl, onClick = { onEditEntry(entry) })
            }
            if (entries.size > 3) {
                Text(
                    text = "+${entries.size - 3} more today",
                    style = SippyType.caption,
                    color = c.text3,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FriendsInCard(
            state = state,
            friends = loose,
            goalMetToday = goalMet,
            hasEntriesToday = hasEntries,
            animate = animate,
            onFriendTap = onFriendTap,
            onOpenFriends = onOpenFriends,
        )
    }
}

/** A flat row: the glass belongs to the card, not to each line. */
@Composable
private fun LogRow(entry: LogEntry, goalMl: Int, onClick: () -> Unit) {
    val c = LocalSippyColors.current
    val drink = DrinkTable.byId(entry.drinkId)
    val percent = if (goalMl > 0) {
        (entry.hydratedMl.toFloat() / goalMl * 100f).roundToInt()
    } else {
        0
    }
    val time = Instant.ofEpochMilli(entry.atEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormat)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.touch)
            .clip(RoundedCornerShape(Dimens.inner))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${drink.name}, ${entry.volumeMl} millilitres at $time, " +
                    "$percent percent of the goal. Double tap to edit."
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrinkIcon(drinkId = entry.drinkId, size = 34.dp, label = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = drink.name,
                style = SippyType.emphasis,
                color = c.text1,
                maxLines = 2,
            )
            Text(
                text = "${entry.volumeMl} ml · $time",
                style = SippyType.caption,
                color = c.text2,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = SippyType.caption,
            color = c.accentBright,
        )
    }
}

/** The friends that are not on the orb: up to four, then "Friends ›". */
@Composable
private fun FriendsInCard(
    state: StoredState,
    friends: List<Friend>,
    goalMetToday: Boolean,
    hasEntriesToday: Boolean,
    animate: Boolean,
    onFriendTap: (Friend) -> Unit,
    onOpenFriends: () -> Unit,
) {
    val c = LocalSippyColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (friends.isEmpty()) {
                Text(
                    text = "No one else yet.",
                    style = SippyType.caption,
                    color = c.text3,
                )
            }
            friends.forEach { friend ->
                val mood = moodFor(friend, state, goalMetToday, hasEntriesToday)
                val worn = ShopRules.worn(
                    state.equipped,
                    state.equippedOutfits,
                    state.ownedItems,
                    friend.id,
                )
                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = Dimens.touch, minHeight = Dimens.touch)
                        .clip(RoundedCornerShape(Dimens.inner))
                        .clickable { onFriendTap(friend) }
                        .semantics { contentDescription = "${friend.name}, ${mood.name.lowercase()}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FriendAvatar(
                        friend = friend,
                        mood = mood,
                        size = 46.dp,
                        animate = animate,
                        hat = worn.firstOrNull { it.slot == ItemSlot.HAT },
                        outfit = worn.firstOrNull { it.slot == ItemSlot.OUTFIT },
                    )
                    Text(friend.name, style = SippyType.caption, color = c.text2, maxLines = 1)
                }
            }
        }
        Text(
            text = "Friends ›",
            style = SippyType.caption,
            color = c.text3,
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.touch)
                .clip(RoundedCornerShape(Dimens.pill))
                .clickable(onClick = onOpenFriends)
                .semantics { contentDescription = "Open friends" }
                .padding(horizontal = 8.dp, vertical = 14.dp),
        )
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

/** One line per mood, shown in the orb's speech bubble. */
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
