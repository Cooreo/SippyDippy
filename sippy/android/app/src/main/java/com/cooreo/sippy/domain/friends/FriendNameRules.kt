package com.cooreo.sippy.domain.friends

/**
 * Friend names are 1..20 characters, counted in code points so an emoji is one
 * character, not two. Pure, so the rules are unit tested on the JVM.
 */
object FriendNameRules {
    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 20

    /** Trimmed, inner whitespace collapsed, and never longer than the limit. */
    fun sanitize(raw: String): String {
        val trimmed = raw.trim().replace(Whitespace, " ")
        if (length(trimmed) <= MAX_LENGTH) return trimmed
        // Walk forward by code points so a cut never splits a surrogate pair.
        var index = 0
        var count = 0
        while (index < trimmed.length && count < MAX_LENGTH) {
            index += Character.charCount(trimmed.codePointAt(index))
            count += 1
        }
        return trimmed.substring(0, index).trim()
    }

    private val Whitespace = Regex("\\s+")

    fun length(raw: String): Int {
        val trimmed = raw.trim()
        return trimmed.codePointCount(0, trimmed.length)
    }

    fun isValid(raw: String): Boolean = length(raw) in MIN_LENGTH..MAX_LENGTH

    /** One user-facing sentence, or null when the name is fine. */
    fun error(raw: String): String? = when {
        length(raw) < MIN_LENGTH -> "Give the friend a name."
        length(raw) > MAX_LENGTH -> "Names are $MIN_LENGTH to $MAX_LENGTH characters."
        else -> null
    }

    fun defaultNameFor(kind: com.cooreo.sippy.model.FriendKind): String = when (kind) {
        com.cooreo.sippy.model.FriendKind.DIPPY -> "Dippy"
        com.cooreo.sippy.model.FriendKind.PLANT -> "Sprout"
        com.cooreo.sippy.model.FriendKind.CAT -> "Mochi"
    }
}
