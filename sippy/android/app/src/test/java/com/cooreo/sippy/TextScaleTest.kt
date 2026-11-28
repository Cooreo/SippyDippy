package com.cooreo.sippy

import com.cooreo.sippy.ui.theme.SippyType
import com.cooreo.sippy.ui.theme.cappedFontScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 9, 1.2: the type scale, held to its own rule. */
class TextScaleTest {

    @Test
    fun theScaleMatchesTheBrief() {
        assertEquals(28f, SippyType.title.fontSize.value, 0.01f)
        assertEquals(18f, SippyType.section.fontSize.value, 0.01f)
        assertEquals(15f, SippyType.body.fontSize.value, 0.01f)
        assertEquals(13f, SippyType.secondary.fontSize.value, 0.01f)
        assertEquals(12f, SippyType.label.fontSize.value, 0.01f)
        assertEquals(11f, SippyType.tab.fontSize.value, 0.01f)
    }

    @Test
    fun nothingIsSmallerThanElevenSp() {
        val roles = listOf(
            "display" to SippyType.display,
            "title" to SippyType.title,
            "value" to SippyType.value,
            "emphasis" to SippyType.emphasis,
            "body" to SippyType.body,
            "caption" to SippyType.caption,
            "label" to SippyType.label,
            "section" to SippyType.section,
            "secondary" to SippyType.secondary,
            "tab" to SippyType.tab,
        )
        for ((name, style) in roles) {
            assertTrue("$name is ${style.fontSize}", style.fontSize.value >= 11f)
        }
    }

    @Test
    fun decorativeLabelsAreCapped() {
        assertEquals(1.0f, cappedFontScale(1.0f), 0f)
        assertEquals(1.3f, cappedFontScale(1.3f), 0f)
        assertEquals(1.3f, cappedFontScale(2.0f), 0f)
        // Body copy is not capped: it scales fully, containers grow instead.
        assertEquals(2.0f, cappedFontScale(2.0f, max = 2.0f), 0f)
    }
}
