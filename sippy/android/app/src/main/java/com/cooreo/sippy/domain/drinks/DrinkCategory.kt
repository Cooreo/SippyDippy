package com.cooreo.sippy.domain.drinks

/**
 * The ten families a drink can belong to (round 8, B3/B4): one glass tint and one
 * icon each. Derived from the drink id, so the table stays the single source of
 * truth for what exists and this file only groups it.
 */
enum class DrinkCategory {
    WATER,
    SPARKLING,
    TEA,
    COFFEE,
    JUICE,
    MILK,
    SMOOTHIE,
    SODA,
    ALCOHOL,
    OTHER,
    ;

    companion object {
        /**
         * Everything the table ships with, mapped once. Unknown ids fall to
         * [OTHER] rather than throwing — the table is data, not a contract.
         */
        fun of(drinkId: String): DrinkCategory = when (drinkId) {
            "water", "mineral", "coconut", "electrolyte", "flavoured-water" -> WATER
            "sparkling", "sports" -> SPARKLING
            "tea", "green-tea", "herbal-tea", "iced-tea", "kombucha" -> TEA
            "coffee", "espresso", "latte" -> COFFEE
            "juice", "apple-juice", "lemonade" -> JUICE
            "milk", "oat-milk", "almond-milk", "kefir" -> MILK
            "smoothie" -> SMOOTHIE
            "soda", "diet-soda", "energy" -> SODA
            "beer", "wine", "cocktail" -> ALCOHOL
            else -> OTHER
        }

        /** The drawable name for a category, or null when no art exists yet. */
        fun iconName(category: DrinkCategory): String? = when (category) {
            WATER -> "drink_water"
            SPARKLING -> "drink_sparkling"
            TEA -> "drink_tea"
            COFFEE -> "drink_coffee"
            JUICE -> "drink_juice"
            MILK -> "drink_milk"
            SMOOTHIE -> "drink_smoothie"
            SODA -> "drink_soda"
            ALCOHOL -> "drink_alcohol"
            OTHER -> "drink_other"
        }
    }
}
