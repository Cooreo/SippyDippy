package com.cooreo.sippy.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * Where the water orb currently is, in root coordinates (round 8, B7).
 *
 * The droplet that flies out of the Log sheet has to land in the orb, and the
 * orb lives several screens away in the composition. Rather than plumb a
 * callback through Home, the orb reports its own rectangle here; the overlay that
 * draws the droplet reads it. One window, one orb, so a process-wide holder is
 * enough — and it is a `mutableStateOf`, so the read is a normal Compose read.
 */
object OrbAnchor {
    var bounds: Rect by mutableStateOf(Rect.Zero)
}
