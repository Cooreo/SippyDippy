package com.cooreo.sippy

import com.cooreo.sippy.domain.friends.GrowthLevel
import com.cooreo.sippy.domain.friends.IdleMotion
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.MoodLines
import com.cooreo.sippy.model.FriendKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B9: a line per friend, at least six per species, and no two identical. */
class MoodLinesTest {

    @Test
    fun everKindAndMoodHasALine() {
        for (kind in FriendKind.entries) {
            for (mood in Mood.entries) {
                val line = MoodLines.line(kind, mood, "friend-1")
                assertTrue("$kind/$mood is blank", line.isNotBlank())
                assertTrue("$kind/$mood still has a wrapper: $line", !line.contains("{"))
            }
        }
    }

    @Test
    fun everySpeciesSaysAtLeastSixThings() {
        for (kind in FriendKind.entries) {
            val all = Mood.entries.flatMap { MoodLines.lines(kind, it) }
            assertTrue("$kind has only ${all.size}", all.size >= 6)
            assertEquals("$kind repeats a line", all.size, all.toSet().size)
        }
    }

    @Test
    fun noLineIsSharedAcrossTheWholeZoo() {
        val all = FriendKind.entries.flatMap { kind ->
            Mood.entries.flatMap { MoodLines.lines(kind, it) }
        }
        assertEquals("some friend says another friend's sentence", all.size, all.toSet().size)
    }

    @Test
    fun twoFriendsOfOneSpeciesDoNotSayTheSameThing() {
        for (kind in FriendKind.entries) {
            val first = MoodLines.line(kind, Mood.HAPPY, "a1")
            val second = MoodLines.line(kind, Mood.HAPPY, "b2")
            assertTrue("$kind echoed itself", first != second)
        }
    }

    @Test
    fun theSameFriendIsStable() {
        assertEquals(
            MoodLines.line(FriendKind.KOI, Mood.SLEEPY, "koi-7"),
            MoodLines.line(FriendKind.KOI, Mood.SLEEPY, "koi-7"),
        )
    }

    @Test
    fun growthIsMonotonicAndClamped() {
        var lastScale = 0f
        var lastGlow = -1f
        for (days in listOf(0, 3, 7, 14, 30, 100, 400)) {
            val scale = GrowthLevel.scale(days)
            val glow = GrowthLevel.glow(days)
            assertTrue("scale shrank at $days", scale >= lastScale)
            assertTrue("glow shrank at $days", glow >= lastGlow)
            assertTrue("scale out of range at $days", scale in 1f..1.2f)
            assertTrue("glow out of range at $days", glow in 0f..0.5f)
            lastScale = scale
            lastGlow = glow
        }
        assertEquals(null, GrowthLevel.label(0))
        assertTrue(GrowthLevel.label(14)!!.contains("14"))
    }

    @Test
    fun idleMotionIsBoundedAndContinuous() {
        var previousBob = IdleMotion.bob(0f)
        var maxHop = 0f
        var steps = 0
        while (steps <= 1000) {
            val cycle = steps / 1000f
            val bob = IdleMotion.bob(cycle)
            val hop = IdleMotion.hop(cycle)
            assertTrue("bob out of range", kotlin.math.abs(bob) <= 1.6f)
            assertTrue("hop out of range", hop in 0f..6f)
            assertTrue("bob jumped at $cycle", kotlin.math.abs(bob - previousBob) < 0.1f)
            if (hop > maxHop) maxHop = hop
            previousBob = bob
            steps += 1
        }
        assertTrue("the hop never happens", maxHop > 4f)
        assertEquals("hop at rest", 0f, IdleMotion.hop(0f), 0f)
    }

    @Test
    fun aTapSqueezesAndReturns() {
        assertEquals(1f, IdleMotion.tapScaleY(0f), 0.001f)
        assertEquals(1f, IdleMotion.tapScaleY(1f), 0.001f)
        assertTrue(IdleMotion.tapScaleY(0.5f) < 0.9f)
        assertTrue(IdleMotion.tapScaleX(0.5f) > 1.05f)
    }
}
