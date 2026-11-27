package com.cooreo.sippy.ui.friends

import com.cooreo.sippy.domain.shop.ItemSlot
import com.cooreo.sippy.model.FriendKind

/**
 * Where a cosmetic sits on a friend, relative to that friend's art box (round 8,
 * A4). One table, one file: the tuner in Developer options edits these numbers,
 * and nothing else may hard-code an offset.
 *
 * [dx]/[dy] are fractions of the art box measured from its centre, [scale] is the
 * overlay's width as a fraction of the box, [rotationDeg] is clockwise.
 */
data class OverlayAnchor(
    val dx: Float,
    val dy: Float,
    val scale: Float,
    val rotationDeg: Float,
)

object Anchors {

    /** Per species, per slot. First-pass values; tune on the phone, then paste here. */
    private val table: Map<Pair<FriendKind, ItemSlot>, OverlayAnchor> = mapOf(
        (FriendKind.DIPPY to ItemSlot.HAT) to OverlayAnchor(0f, -0.30f, 0.46f, -6f),
        (FriendKind.DIPPY to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.16f, 0.42f, 0f),
        (FriendKind.PLANT to ItemSlot.HAT) to OverlayAnchor(0f, -0.30f, 0.44f, 4f),
        (FriendKind.PLANT to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.30f, 0.40f, 0f),
        (FriendKind.CAT to ItemSlot.HAT) to OverlayAnchor(0f, -0.32f, 0.44f, -4f),
        (FriendKind.CAT to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.22f, 0.40f, 0f),
        (FriendKind.KOI to ItemSlot.HAT) to OverlayAnchor(-0.04f, -0.30f, 0.40f, 8f),
        (FriendKind.KOI to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.12f, 0.36f, 0f),
        (FriendKind.OTTER to ItemSlot.HAT) to OverlayAnchor(0f, -0.33f, 0.46f, 0f),
        (FriendKind.OTTER to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.20f, 0.42f, 0f),
        (FriendKind.TURTLE to ItemSlot.HAT) to OverlayAnchor(0f, -0.34f, 0.48f, 0f),
        (FriendKind.TURTLE to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.18f, 0.44f, 0f),
        (FriendKind.AXOLOTL to ItemSlot.HAT) to OverlayAnchor(0f, -0.36f, 0.44f, -6f),
        (FriendKind.AXOLOTL to ItemSlot.OUTFIT) to OverlayAnchor(0f, 0.22f, 0.40f, 0f),
    )

    private val fallback = OverlayAnchor(0f, -0.30f, 0.44f, 0f)

    fun forSpecies(kind: FriendKind, slot: ItemSlot): OverlayAnchor =
        table[kind to slot] ?: fallback

    /** Every row, so the tuner can walk the table and the report can print it. */
    fun rows(): List<Triple<FriendKind, ItemSlot, OverlayAnchor>> =
        FriendKind.entries.flatMap { kind ->
            ItemSlot.entries.map { slot -> Triple(kind, slot, forSpecies(kind, slot)) }
        }
}
