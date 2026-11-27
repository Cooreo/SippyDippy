package com.cooreo.sippy.ui.theme

import androidx.compose.ui.graphics.Color
import com.cooreo.sippy.domain.drinks.DrinkCategory

/**
 * Card tint by content (round 8, B3): water blue, tea green, coffee amber, juice
 * orange — and a reading for the rest of the table so a row never falls back to
 * a neutral grey. Kept at a low alpha so the glass stays glass.
 */
object DrinkTints {

    fun of(category: DrinkCategory): Color = when (category) {
        DrinkCategory.WATER -> Color(0xFF2E7CF6)
        DrinkCategory.SPARKLING -> Color(0xFF52B6F0)
        DrinkCategory.TEA -> Color(0xFF2FA971)
        DrinkCategory.COFFEE -> Color(0xFFB7762E)
        DrinkCategory.JUICE -> Color(0xFFE9873C)
        DrinkCategory.MILK -> Color(0xFFE6DCCB)
        DrinkCategory.SMOOTHIE -> Color(0xFFD96FA7)
        DrinkCategory.SODA -> Color(0xFF4BC7C7)
        DrinkCategory.ALCOHOL -> Color(0xFF8C6BE0)
        DrinkCategory.OTHER -> Color(0xFF6E8AA8)
    }

    /** The alpha a card uses so the tint reads without hiding the backdrop. */
    const val CARD_ALPHA = 0.16f

    fun card(category: DrinkCategory): Color = of(category).copy(alpha = CARD_ALPHA)
}
