package com.cooreo.sippy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.ui.theme.LocalSippyColors

/**
 * Walking skeleton — replaced by the real screen stack in the UI step.
 * It exists so CI can prove the toolchain (Gradle + AGP + Kotlin + Compose)
 * works end to end before the app code lands.
 */
@Composable
fun SippyRoot() {
    val c = LocalSippyColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(c.bg0, c.bg1),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Sippy ${BuildConfig.VERSION_NAME}", color = c.text1)
    }
}
