package com.cooreo.sippy

import android.app.Application

/**
 * Sippy's Application.
 *
 * First-run guard #1 (brief): [onCreate] does NO network and NO blocking I/O.
 * The container is created lazily; the repository loads on a background
 * dispatcher behind the splash, and every read path is total (a corrupt or
 * unknown-version file can never crash the process).
 */
class SippyApp : Application()
