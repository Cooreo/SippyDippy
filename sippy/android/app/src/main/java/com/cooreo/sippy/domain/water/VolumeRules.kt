package com.cooreo.sippy.domain.water

/**
 * Volume validation shared by the quick-add chips, the custom-volume field and
 * the entry editor. Pure Kotlin, so the boundaries are unit tested on the JVM.
 */
object VolumeRules {
    const val MIN_ML = 50
    const val MAX_ML = 2000

    /** The three quick-add chips on Home. */
    val PRESETS: List<Int> = listOf(250, 330, 500)

    fun isValid(ml: Int): Boolean = ml in MIN_ML..MAX_ML

    fun clamp(ml: Int): Int = ml.coerceIn(MIN_ML, MAX_ML)

    /**
     * Parses what the user typed: digits only, no sign, no decimals, no
     * thousands separator. Returns null for anything that is not a whole,
     * in-range millilitre value — callers show [error] instead of guessing.
     */
    fun parse(text: String): Int? {
        val digits = text.trim()
        if (digits.isEmpty() || digits.length > 4) return null
        if (!digits.all { it in '0'..'9' }) return null
        val ml = digits.toIntOrNull() ?: return null
        return if (isValid(ml)) ml else null
    }

    /** One user-facing sentence for the custom field, or null when it parses. */
    fun error(text: String): String? = when {
        text.isBlank() -> "Enter a volume in millilitres."
        parse(text) == null -> "Use $MIN_ML to $MAX_ML ml."
        else -> null
    }

    fun label(ml: Int): String = "$ml ml"
}
