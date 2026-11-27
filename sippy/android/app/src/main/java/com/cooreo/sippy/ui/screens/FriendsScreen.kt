package com.cooreo.sippy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.domain.friends.GrowthLevel
import com.cooreo.sippy.domain.friends.IdleMotion
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodLines
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.friends.PinRules
import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FriendAvatar
import com.cooreo.sippy.ui.glass.GlassCard
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType

@Composable
fun FriendsScreen(
    state: StoredState,
    controller: SippyController,
    hydratedMl: Int,
    goalMl: Int,
    animate: Boolean,
    onTogglePin: (String) -> Unit,
    onOpenShop: () -> Unit = {},
    onDressUp: (Friend) -> Unit = {},
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
                    Text("Friends", style = SippyType.title, color = c.text1)
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
            val worn = ShopRules.worn(
                state.equipped,
                state.equippedOutfits,
                state.ownedItems,
                friend.id,
            )
            FriendCard(
                friend = friend,
                pinLabel = PinRules.pinLabel(state.friends, friend.id),
                onTogglePin = { onTogglePin(friend.id) },
                hat = worn.firstOrNull { it.slot == ItemSlot.HAT },
                outfit = worn.firstOrNull { it.slot == ItemSlot.OUTFIT },
                onDressUp = { onDressUp(friend) },
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
    pinLabel: String,
    onTogglePin: () -> Unit,
    hat: ShopItem?,
    outfit: ShopItem?,
    onDressUp: () -> Unit,
    mood: Mood,
    animate: Boolean,
    isRenaming: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onStartRename: () -> Unit,
    onSubmitRename: () -> Unit,
) {
    val c = LocalSippyColors.current
    val glowAlpha = GrowthLevel.glow(friend.grownAtStreak)
    val growthScale = GrowthLevel.scale(friend.grownAtStreak)

    // B9: the idle cycle and the tap reaction are Animatables read inside the
    // layer lambda below, so a bobbing friend costs a layer update, not a
    // recomposition of the card.
    val idle = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (animate) {
            idle.snapTo(0f)
            idle.animateTo(1f, infiniteRepeatable(tween(IdleMotion.CYCLE_MS, easing = LinearEasing)))
        } else {
            idle.snapTo(0f)
        }
    }
    val tap = remember { Animatable(0f) }
    val tapToken = remember { mutableIntStateOf(0) }
    LaunchedEffect(tapToken.intValue) {
        if (tapToken.intValue > 0 && animate) {
            tap.snapTo(0f)
            tap.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tap reacts, long-press pins; the Pin chip is the same action with a
            // TalkBack label.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .pointerInput(friend.id) {
                        detectTapGestures(
                            onTap = { tapToken.intValue += 1 },
                            onLongPress = { onTogglePin() },
                        )
                    }
                    .graphicsLayer {
                        val squeezeX = IdleMotion.tapScaleX(tap.value)
                        val squeezeY = IdleMotion.tapScaleY(tap.value)
                        scaleX = growthScale * squeezeX
                        scaleY = growthScale * squeezeY
                        val lift = IdleMotion.bob(idle.value) + IdleMotion.hop(idle.value)
                        translationY = lift * density
                    }
                    .drawBehind {
                        if (glowAlpha > 0f) {
                            // Growth is a soft halo, not a blur: one radial wash.
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c.accentBright.copy(alpha = glowAlpha),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.78f,
                                ),
                                radius = size.minDimension * 0.78f,
                                center = center,
                            )
                        }
                        // Sparkle burst on tap: eight dots flying out and fading.
                        val t = tap.value
                        if (t > 0f && t < 1f) {
                            val alpha = (1f - t).coerceIn(0f, 1f)
                            val reach = size.minDimension * (0.34f + 0.32f * t)
                            repeat(8) { index ->
                                val angle = index * (Math.PI / 4.0)
                                drawCircle(
                                    color = c.accentBright.copy(alpha = alpha * 0.85f),
                                    radius = size.minDimension * 0.022f,
                                    center = Offset(
                                        x = center.x + (kotlin.math.cos(angle) * reach).toFloat(),
                                        y = center.y + (kotlin.math.sin(angle) * reach).toFloat(),
                                    ),
                                )
                            }
                        }
                    },
            ) {
                FriendAvatar(
                    friend = friend,
                    mood = mood,
                    size = 88.dp,
                    animate = animate,
                    hat = hat,
                    outfit = outfit,
                )
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
                        text = MoodLines.line(friend.kind, mood, friend.id),
                        color = c.text2,
                        fontSize = 12.sp,
                    )
                    GrowthLevel.label(friend.grownAtStreak)?.let { growth ->
                        Text(growth, color = c.text3, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(label = "Dress up", onClick = onDressUp)
                        GlassChip(label = "Rename", onClick = onStartRename)
                        GlassChip(
                            label = pinLabel,
                            selected = friend.pinned,
                            onClick = onTogglePin,
                        )
                    }
                }
            }
        }
    }
}
