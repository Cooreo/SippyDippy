package com.cooreo.sippy

import com.cooreo.sippy.domain.water.VolumeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeRulesTest {

    @Test
    fun presetsAreAllValid() {
        assertEquals(listOf(250, 330, 500), VolumeRules.PRESETS)
        VolumeRules.PRESETS.forEach { assertTrue("$it should be valid", VolumeRules.isValid(it)) }
    }

    @Test
    fun boundariesAreInclusive() {
        assertTrue(VolumeRules.isValid(VolumeRules.MIN_ML))
        assertTrue(VolumeRules.isValid(VolumeRules.MAX_ML))
        assertFalse(VolumeRules.isValid(VolumeRules.MIN_ML - 1))
        assertFalse(VolumeRules.isValid(VolumeRules.MAX_ML + 1))
    }

    @Test
    fun parseAcceptsWholeNumbersInRange() {
        assertEquals(250, VolumeRules.parse("250"))
        assertEquals(50, VolumeRules.parse("50"))
        assertEquals(2000, VolumeRules.parse("2000"))
        assertEquals(250, VolumeRules.parse("  250  "))
        assertEquals(250, VolumeRules.parse("0250"))
    }

    @Test
    fun parseRejectsWhatTheUserMustFix() {
        assertNull(VolumeRules.parse(""))
        assertNull(VolumeRules.parse("   "))
        assertNull(VolumeRules.parse("49"))
        assertNull(VolumeRules.parse("2001"))
        assertNull(VolumeRules.parse("1 000"))
        assertNull(VolumeRules.parse("12.5"))
        assertNull(VolumeRules.parse("-250"))
        assertNull(VolumeRules.parse("250ml"))
        assertNull(VolumeRules.parse("12345"))
    }

    @Test
    fun errorTextExplainsTheRange() {
        assertEquals("Enter a volume in millilitres.", VolumeRules.error(""))
        assertEquals("Use 50 to 2000 ml.", VolumeRules.error("2001"))
        assertNull(VolumeRules.error("330"))
    }

    @Test
    fun clampKeepsTypedValuesInsideTheRange() {
        assertEquals(50, VolumeRules.clamp(0))
        assertEquals(2000, VolumeRules.clamp(9999))
        assertEquals(330, VolumeRules.clamp(330))
    }

    @Test
    fun labelIsMillilitres() {
        assertEquals("330 ml", VolumeRules.label(330))
    }
}
