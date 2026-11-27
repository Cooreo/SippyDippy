package com.cooreo.sippy.domain.friends

import com.cooreo.sippy.model.FriendKind
import kotlin.math.absoluteValue

/**
 * What a friend says about how it feels (round 8, B9).
 *
 * Two lines per mood per species, chosen by the friend's id, so two koi with the
 * same mood never say the same sentence — and a rename never changes what a
 * friend is like, because the id is what picks the line. Four moods × two lines
 * is eight per species, above the six the round asks for.
 */
object MoodLines {

    fun line(kind: FriendKind, mood: Mood, friendId: String): String {
        val options = lines(kind, mood)
        val index = friendId.hashCode().absoluteValue % options.size
        return options[index]
    }

    fun lines(kind: FriendKind, mood: Mood): List<String> = when (mood) {
        Mood.SLEEPY -> sleepy(kind)
        Mood.HAPPY -> happy(kind)
        Mood.DANCING -> dancing(kind)
        Mood.SAD -> sad(kind)
    }

    private fun sleepy(kind: FriendKind): List<String> = when (kind) {
        FriendKind.DIPPY -> listOf(
            "Still dozing. One sip and I am up.",
            "Wake me when the glass is full.",
        )
        FriendKind.PLANT -> listOf(
            "Leaves drooping. Water, please.",
            "I photosynthesise slower when you forget me.",
        )
        FriendKind.CAT -> listOf(
            "Curled into a comma. No water, no purring.",
            "I sleep until the bowl has water in it.",
        )
        FriendKind.KOI -> listOf(
            "Hovering near the bottom, waiting for the level to rise.",
            "Water is my whole world. It is a bit low in here.",
        )
        FriendKind.OTTER -> listOf(
            "Floating on my back, half asleep. Wake me with a splash.",
            "No sips yet, so I am napping on the surface.",
        )
        FriendKind.TURTLE -> listOf(
            "Pulled into my shell until the first sip lands.",
            "Slow morning. Even slower without water.",
        )
        FriendKind.AXOLOTL -> listOf(
            "Gills curled, eyes shut. Sip soon?",
            "Dreaming of a full glass. Still dreaming.",
        )
    }

    private fun happy(kind: FriendKind): List<String> = when (kind) {
        FriendKind.DIPPY -> listOf(
            "Happy and hydrated. That is the whole mood.",
            "Today counts. I counted it myself.",
        )
        FriendKind.PLANT -> listOf(
            "Standing up straight, leaves glossy.",
            "Perked right up. Keep it coming.",
        )
        FriendKind.CAT -> listOf(
            "Purring at the water line. Acceptable.",
            "Tail up. You did the thing.",
        )
        FriendKind.KOI -> listOf(
            "Circling the glass happily, fins out.",
            "The water is high and so am I.",
        )
        FriendKind.OTTER -> listOf(
            "Doing backflips. Water is a good look on you.",
            "Splashing about. Good sips today.",
        )
        FriendKind.TURTLE -> listOf(
            "Paddling in circles of approval.",
            "Shell is polished. Hydration does that.",
        )
        FriendKind.AXOLOTL -> listOf(
            "Gills waving hello. Nicely done.",
            "Smiling with my whole face. Sip by sip.",
        )
    }

    private fun dancing(kind: FriendKind): List<String> = when (kind) {
        FriendKind.DIPPY -> listOf(
            "Goal met and the streak is holding. Dancing.",
            "Full glass, longer streak. This is my song.",
        )
        FriendKind.PLANT -> listOf(
            "Doing a little leaf shuffle. Streak alive.",
            "Every leaf is dancing. Water does that.",
        )
        FriendKind.CAT -> listOf(
            "Weaving figure eights. Streak intact, obviously.",
            "Zoomies. The streak earns zoomies.",
        )
        FriendKind.KOI -> listOf(
            "Spiralling through the full glass. Streak kept.",
            "Fins out, tail flicking. Another day won.",
        )
        FriendKind.OTTER -> listOf(
            "Barrel rolls in celebration. Streak is safe.",
            "Twisting through the water. Goal reached again.",
        )
        FriendKind.TURTLE -> listOf(
            "Turtle breakdance. Slow, but a dance.",
            "Paddling in time. The streak continues.",
        )
        FriendKind.AXOLOTL -> listOf(
            "Wiggling all four legs. Streak stays.",
            "Swaying gills to the beat. Goal met.",
        )
    }

    private fun sad(kind: FriendKind): List<String> = when (kind) {
        FriendKind.DIPPY -> listOf(
            "The streak slipped. I will wait here.",
            "Missed a day. Water will cheer me up.",
        )
        FriendKind.PLANT -> listOf(
            "A leaf went yellow. Let us not talk about it.",
            "Wilting a little. A glass would help.",
        )
        FriendKind.CAT -> listOf(
            "Sitting with my back to you. On purpose.",
            "Ears flat. The streak was going so well.",
        )
        FriendKind.KOI -> listOf(
            "Swimming low and slow. The streak broke.",
            "Quiet in the water. I noticed the empty day.",
        )
        FriendKind.OTTER -> listOf(
            "Not floating. Sitting. That is how bad it is.",
            "No splashing today. We lost the run.",
        )
        FriendKind.TURTLE -> listOf(
            "Head half out of the shell. Not a good day.",
            "Slow and quiet. Tomorrow, yes?",
        )
        FriendKind.AXOLOTL -> listOf(
            "Gills drooping. The streak was a nice one.",
            "Floating still. Sip with me later.",
        )
    }
}
