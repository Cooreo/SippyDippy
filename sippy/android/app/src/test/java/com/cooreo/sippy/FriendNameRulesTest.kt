package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.FriendNameRules
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Friend names are 1..20 characters. Pure JVM. */
class FriendNameRulesTest {

    @Test
    fun oneCharacterIsEnough() {
        assertTrue(FriendNameRules.isValid("D"))
        assertNull(FriendNameRules.error("D"))
    }

    @Test
    fun twentyCharactersAreAllowedAndTwentyOneAreNot() {
        val twenty = "a".repeat(20)
        val twentyOne = "a".repeat(21)
        assertEquals(20, FriendNameRules.length(twenty))
        assertTrue(FriendNameRules.isValid(twenty))
        assertFalse(FriendNameRules.isValid(twentyOne))
        assertNotNull(FriendNameRules.error(twentyOne))
        assertTrue(FriendNameRules.error(twentyOne)!!.contains("20"))
    }

    @Test
    fun blankNamesAreRejectedWithAClearLine() {
        assertFalse(FriendNameRules.isValid(""))
        assertFalse(FriendNameRules.isValid("    "))
        assertEquals("Give the friend a name.", FriendNameRules.error(""))
    }

    @Test
    fun surroundingSpaceIsNotPartOfTheName() {
        assertEquals("Mochi", FriendNameRules.sanitize("  Mochi  "))
        assertEquals("Big Cat", FriendNameRules.sanitize(" Big   Cat "))
        assertTrue(FriendNameRules.isValid("   Mochi   "))
    }

    @Test
    fun overlongInputIsCutToTheLimitWithoutSplittingCharacters() {
        val cut = FriendNameRules.sanitize("a".repeat(30))
        assertEquals(FriendNameRules.MAX_LENGTH, FriendNameRules.length(cut))
        // An emoji is two UTF-16 units but one character: a cut must not halve it.
        val cleaned = FriendNameRules.sanitize("🐳".repeat(19) + "ok")
        assertEquals(FriendNameRules.MAX_LENGTH, FriendNameRules.length(cleaned))
        assertTrue(FriendNameRules.isValid(cleaned))
        assertFalse(Character.isHighSurrogate(cleaned.last()))
        assertTrue(cleaned.startsWith("🐳"))
    }

    @Test
    fun emojiCountAsOneCharacter() {
        val twentyWhales = "🐳".repeat(20)
        assertEquals(20, FriendNameRules.length(twentyWhales))
        assertTrue(FriendNameRules.isValid(twentyWhales))
        assertFalse(FriendNameRules.isValid("🐳".repeat(21)))
    }

    @Test
    fun defaultsMatchTheChosenFriend() {
        assertEquals("Dippy", FriendNameRules.defaultNameFor(FriendKind.DIPPY))
        assertEquals("Sprout", FriendNameRules.defaultNameFor(FriendKind.PLANT))
        assertEquals("Mochi", FriendNameRules.defaultNameFor(FriendKind.CAT))
        FriendKind.entries.forEach { kind ->
            assertTrue(FriendNameRules.isValid(FriendNameRules.defaultNameFor(kind)))
        }
    }
}
