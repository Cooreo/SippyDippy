package com.cooreo.sippy

import android.app.Application
import com.cooreo.sippy.data.AppGraph

/**
 * Sippy's Application.
 *
 * First-run guard #1 (brief): [onCreate] does NO network and NO blocking I/O.
 * The container is created lazily, on first use from an Activity; the
 * repository loads on a background dispatcher behind the splash, and every
 * read path is total (a corrupt or unknown-version file can never crash the
 * process).
 */
class SippyApp : Application() {
    val graph: AppGraph by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppGraph(this) }
}
