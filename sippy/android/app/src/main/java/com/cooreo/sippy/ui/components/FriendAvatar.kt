package com.cooreo.sippy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.ui.friends.Anchors
import com.cooreo.sippy.ui.friends.OverlayAnchor

/**
 * The one way a friend is drawn (round 8, A4): the mascot, then its worn
 * cosmetics placed from [Anchors]. Every surface that shows a friend uses this —
 * Home pins, the Home strip, Friends cards, Shop previews and the dress-up sheet —
 * so a hat sits the same way everywhere.
 */
@Composable
fun FriendAvatar(
    friend: Friend,
    mood: Mood,
    size: Dp,
    animate: Boolean,
    hat: ShopItem? = null,
    outfit: ShopItem? = null,
    modifier: Modifier = Modifier,
    /** Tuner override: replaces the table for the slot of [tuneSlot]. */
    anchorOverride: OverlayAnchor? = null,
    tuneSlot: com.cooreo.sippy.domain.shop.ItemSlot? = null,
) {
    Box(modifier.size(size)) {
        FriendImage(
            kind = friend.kind,
            mood = mood,
            growthStreak = friend.grownAtStreak,
            size = size.value.toInt(),
            animate = animate,
            modifier = Modifier.fillMaxSize(),
        )
        // Outfit under the hat, so a hat that overlaps the collar still reads.
        listOfNotNull(outfit, hat).forEach { item ->
            val tableAnchor = Anchors.forSpecies(item.kind, item.slot)
            val anchor = if (anchorOverride != null && tuneSlot == item.slot) {
                anchorOverride
            } else {
                tableAnchor
            }
            Cosmetic(item = item, anchor = anchor, boxSize = size)
        }
    }
}

@Composable
private fun BoxScope.Cosmetic(item: ShopItem, anchor: OverlayAnchor, boxSize: Dp) {
    val res = itemDrawable(item.id) ?: return
    val px = boxSize.value
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.Center)
            .size((px * anchor.scale).dp)
            .graphicsLayer {
                translationX = anchor.dx * boxSize.toPx()
                translationY = anchor.dy * boxSize.toPx()
                rotationZ = anchor.rotationDeg
            },
    )
}
