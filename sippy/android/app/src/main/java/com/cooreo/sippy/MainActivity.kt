package com.cooreo.sippy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cooreo.sippy.ui.SippyRoot
import com.cooreo.sippy.ui.theme.SippyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SippyTheme {
                SippyRoot()
            }
        }
    }
}
