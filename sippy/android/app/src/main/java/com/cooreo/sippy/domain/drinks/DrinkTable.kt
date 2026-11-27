package com.cooreo.sippy.domain.drinks

/**
 * One drink. [hydrationFactor] is the fraction of the volume that still counts
 * toward the daily goal (water = 1.0). Values are literature-informed estimates
 * for v1 — see PROGRESS.md, every factor not verified against a primary source
 * is flagged there.
 */
data class Drink(
    val id: String,
    val name: String,
    val defaultVolumeMl: Int,
    val hydrationFactor: Float,
    val sugary: Boolean,
    val note: String,
) {
    fun hydratedMl(volumeMl: Int): Int = (volumeMl * hydrationFactor).toInt().coerceAtLeast(0)
}

/**
 * The single drink table (v1). Nothing else in the app hard-codes a drink.
 * 26 entries — supports the site's "24+ drinks" line.
 */
object DrinkTable {
    val drinks: List<Drink> = listOf(
        Drink("water", "Water", 250, 1.00f, false, "Reference drink; counts fully."),
        Drink("sparkling", "Sparkling water", 330, 1.00f, false, "Water with CO2; counts fully."),
        Drink("mineral", "Mineral water", 500, 1.00f, false, "Water with minerals; counts fully."),
        Drink("coconut", "Coconut water", 330, 0.95f, false, "Water + electrolytes + a little sugar."),
        Drink("tea", "Tea", 200, 0.90f, false, "Mostly water; caffeine is mildly diuretic."),
        Drink("green-tea", "Green tea", 200, 0.90f, false, "Low caffeine, mostly water."),
        Drink("herbal-tea", "Herbal tea", 200, 0.95f, false, "Caffeine-free infusion."),
        Drink("iced-tea", "Iced tea", 330, 0.85f, true, "Often sweetened; counts as sugary."),
        Drink("coffee", "Coffee", 150, 0.80f, false, "Caffeine and roast compounds."),
        Drink("espresso", "Espresso", 30, 0.75f, false, "Small volume, high caffeine."),
        Drink("latte", "Latte", 300, 0.85f, false, "Milk-based; the milk counts."),
        Drink("milk", "Milk", 250, 0.90f, false, "~87% water plus protein and fat."),
        Drink("oat-milk", "Oat milk", 250, 0.85f, true, "Plant milk, adds sugars and fibre."),
        Drink("almond-milk", "Almond milk", 250, 0.90f, false, "Mostly water, low protein."),
        Drink("smoothie", "Fruit smoothie", 300, 0.85f, true, "Water + fruit; sugar counts."),
        Drink("juice", "Orange juice", 200, 0.85f, true, "Fruit sugar, no fibre."),
        Drink("apple-juice", "Apple juice", 200, 0.85f, true, "Fruit sugar, no fibre."),
        Drink("lemonade", "Lemonade", 330, 0.80f, true, "Water + sugar + lemon."),
        Drink("kombucha", "Kombucha", 330, 0.85f, true, "Fermented tea; some sugar remains."),
        Drink("kefir", "Kefir", 200, 0.85f, false, "Fermented milk."),
        Drink("sports", "Sports drink", 500, 0.90f, true, "Water + electrolytes + sugar."),
        Drink("soda", "Soda", 330, 0.75f, true, "Water + sugar + caffeine; no nutrients."),
        Drink("diet-soda", "Diet soda", 330, 0.70f, true, "Water + sweeteners + acids."),
        Drink("energy", "Energy drink", 250, 0.70f, true, "Caffeine, sugar, taurine."),
        Drink("beer", "Beer", 330, 0.60f, true, "Alcohol is a net diuretic."),
        Drink("wine", "Wine", 150, 0.55f, true, "Alcohol, low water share."),
        Drink("cocktail", "Cocktail", 200, 0.55f, true, "Alcohol + sugar; hydrate alongside."),
        Drink("soup", "Soup", 300, 0.90f, false, "Broth-based; counts mostly."),
        Drink("electrolyte", "Electrolyte tablet in water", 500, 1.00f, false, "Water with salts."),
        Drink("flavoured-water", "Flavoured water", 500, 0.95f, true, "Water with a little sweetener."),
    )

    private val byId = drinks.associateBy { it.id }

    fun find(id: String): Drink = byId[id] ?: drinks.first()

    /** Quick-add presets from the brief. */
    val presets: List<Int> = listOf(250, 330, 500)
}
