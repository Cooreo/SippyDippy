package com.cooreo.sippy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.R
import com.cooreo.sippy.domain.shop.ShopItem
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * Drawable for a cosmetic id. Kept out of the pure ItemTable so the shop rules
 * stay testable on the JVM.
 */
fun itemDrawable(itemId: String): Int? = when (itemId) {
    "koi_hat" -> R.drawable.item_koi_hat
    "koi_scarf" -> R.drawable.item_koi_scarf
    "otter_hat" -> R.drawable.item_otter_hat
    "turtle_hat" -> R.drawable.item_turtle_hat
    "axolotl_hat" -> R.drawable.item_axolotl_hat
    else -> null
}

@Composable
fun ItemImage(item: ShopItem, size: Dp, modifier: Modifier = Modifier) {
    val res = itemDrawable(item.id) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = item.name,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

/**
 * The equipped cosmetic as a badge on the friend's corner (round 7, B4). Items
 * are generated separately from the mascots, so they are worn as a badge rather
 * than warped onto the body — see PROGRESS.md.
 */
@Composable
fun ItemBadge(item: ShopItem, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val c = LocalSippyColors.current
    val res = itemDrawable(item.id) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = "Wearing ${item.name}",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .background(c.glassTint, CircleShape)
            .padding(4.dp),
    )
}
