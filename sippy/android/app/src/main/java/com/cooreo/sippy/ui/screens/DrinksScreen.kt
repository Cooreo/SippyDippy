package com.cooreo.sippy.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.drinks.Drink
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.ui.components.DrinkIcon
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import kotlin.math.roundToInt

/**
 * The Drinks tab: the drink table as tappable glass rows. Tapping logs the
 * drink's usual pour; the Home entry editor and the Log sheet handle the rest.
 */
@Composable
fun DrinksScreen(
    bottomPadding: Dp,
    onLog: (Drink) -> Unit,
) {
    val c = LocalSippyColors.current
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
            Column {
                Text("Drinks", style = SippyType.title, color = c.text1)
                Text(
                    text = "Every drink counts by what your body actually gets. " +
                        "Tap one to log its usual pour.",
                    style = SippyType.body,
                    color = c.text2,
                )
                Spacer(Modifier.height(2.dp))
            }
        }
        items(DrinkTable.drinks, key = { it.id }) { drink ->
            DrinkRow(drink = drink, onLog = { onLog(drink) })
        }
    }
}

@Composable
private fun DrinkRow(drink: Drink, onLog: () -> Unit) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.inner)
    val percent = (drink.hydrationFactor * 100).roundToInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onLog)
            .semantics {
                contentDescription = "Log ${drink.name}, ${drink.defaultVolumeMl} millilitres, " +
                    "counts $percent percent"
            },
    ) {
        GlassSurface(shape = shape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrinkIcon(drinkId = drink.id, size = 38.dp, label = null)
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
                        text = "${drink.defaultVolumeMl} ml · counts $percent%",
                        style = SippyType.caption,
                        color = c.text2,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.pill))
                        .background(c.accent.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "+${drink.defaultVolumeMl}",
                        style = SippyType.label,
                        color = c.accentBright,
                    )
                }
            }
        }
    }
}
