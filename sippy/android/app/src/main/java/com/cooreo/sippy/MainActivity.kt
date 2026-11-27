package com.cooreo.sippy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cooreo.sippy.ui.SippyRoot
import com.cooreo.sippy.ui.system.SystemBars

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge first, then hide both bars in an immersive-sticky setup:
        // a swipe from an edge shows them transiently, and they fade again.
        enableEdgeToEdge()
        SystemBars.showTransparent(this)
        val graph = (application as SippyApp).graph
        setContent {
            SippyRoot(graph)
        }
    }

    /**
     * The system re-shows the bars after a transient swipe, after a dialog, or
     * when the task returns from the background. Re-hide whenever the window
     * regains focus so the scene stays edge-to-edge.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemBars.showTransparent(this)
    }
}
