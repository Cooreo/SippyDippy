package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FriendImage
import com.cooreo.sippy.ui.components.ItemBadge
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.LocalSippyColors

@Composable
fun FriendsScreen(
    state: StoredState,
    controller: SippyController,
    hydratedMl: Int,
    goalMl: Int,
    animate: Boolean,
    onTogglePin: (String) -> Unit,
    onOpenShop: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val c = LocalSippyColors.current
    val goalMet = goalMl > 0 && hydratedMl >= goalMl
    var renaming by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Friends", color = c.text1, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Dippy and up to four friends. They grow with your streak.",
                        color = c.text2,
                        fontSize = 13.sp,
                    )
                }
                // A1: both chips size to their text; the column above absorbs any
                // squeeze instead.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(label = "${state.points} pts", onClick = onOpenShop)
                    GlassChip(label = "Shop", onClick = onOpenShop)
                }
            }
        }
        items(state.friends, key = { it.id }) { friend ->
            val mood = controller.moodOverride ?: MoodRules.forDay(
                goalMetToday = goalMet,
                hasEntriesToday = true,
                currentStreakDays = state.streak.currentDays,
                yesterdayMissedAfterStreak = friend.grownAtStreak >= 3 &&
                    state.streak.currentDays < friend.grownAtStreak,
            )
            FriendCard(
                friend = friend,
                onTogglePin = { onTogglePin(friend.id) },
                worn = ShopRules.wornBy(state.equipped, state.ownedItems, friend.id),
                mood = mood,
                animate = animate,
                isRenaming = renaming == friend.id,
                draft = draft,
                onDraft = { draft = it },
                onStartRename = {
                    renaming = friend.id
                    draft = friend.name
                },
                onSubmitRename = {
                    controller.renameFriend(friend.id, draft)
                    renaming = null
                },
            )
        }
        item {
            GlassCard {
                Text("Add a friend", color = c.text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.friends.size >= Friend.MAX_FRIENDS) {
                        "You have all five friends. Rename them any time."
                    } else {
                        "${Friend.MAX_FRIENDS - state.friends.size} slot(s) left."
                    },
                    color = c.text3,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                // Every kind the player does not own yet, two per row.
                val missing = FriendKind.entries.filter { kind ->
                    kind != FriendKind.DIPPY && state.friends.none { it.kind == kind }
                }
                // Three per row, each chip at its natural width: weighting them
                // to equal shares is what turned "Axolotl" into "A…" at 1.5x.
                missing.chunked(3).forEach { trio ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        trio.forEach { kind ->
                            GlassChip(
                                label = kind.short,
                                onClick = { controller.addFriend(kind) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    onTogglePin: () -> Unit,
    worn: ShopItem?,
    mood: Mood,
    animate: Boolean,
    isRenaming: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onStartRename: () -> Unit,
    onSubmitRename: () -> Unit,
) {
    val c = LocalSippyColors.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Long-press the friend to pin or unpin it; the Pin chip below is
            // the same action with a TalkBack label (B2).
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .pointerInput(friend.id, friend.pinned) {
                        detectTapGestures(onLongPress = { onTogglePin() })
                    },
            ) {
                FriendImage(
                    kind = friend.kind,
                    mood = mood,
                    growthStreak = friend.grownAtStreak,
                    size = 88,
                    animate = animate,
                    modifier = Modifier.size(88.dp),
                )
                if (worn != null) {
                    ItemBadge(item = worn, modifier = Modifier.align(Alignment.BottomEnd), size = 32.dp)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                if (isRenaming) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraft,
                        singleLine = true,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    GlassChip(label = "Save", onClick = onSubmitRename, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(friend.name, color = c.text1, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = moodLine(mood, friend.grownAtStreak),
                        color = c.text3,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(label = "Rename", onClick = onStartRename)
                        GlassChip(
                            label = if (friend.pinned) "Unpin" else "Pin",
                            selected = friend.pinned,
                            onClick = onTogglePin,
                        )
                    }
                }
            }
        }
    }
}

private fun moodLine(mood: Mood, grownAtStreak: Int): String {
    val growth = if (grownAtStreak >= 3) " grown at $grownAtStreak days" else " still small"
    return when (mood) {
        Mood.DANCING -> "Dancing — you are on a roll.$growth"
        Mood.HAPPY -> "Happy — goal met today.$growth"
        Mood.SLEEPY -> "Sleepy — nothing logged yet.$growth"
        Mood.SAD -> "Sad — the streak slipped.$growth"
    }
}
