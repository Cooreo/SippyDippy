package com.cooreo.sippy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Accent choices (Sippy Blue is the default; three more in Settings). */
enum class Accent(val label: String, val seed: Color, val bright: Color) {
    BLUE("Sippy Blue", Color(0xFF2E7CF6), Color(0xFF5B9BFF)),
    TEAL("Lagoon", Color(0xFF12A594), Color(0xFF3FD6C1)),
    VIOLET("Violet", Color(0xFF7C5CFF), Color(0xFFA78BFA)),
    CORAL("Coral", Color(0xFFF26D6D), Color(0xFFFF9A8F)),
}

/** Persisted accent id -> theme accent. */
fun accentFor(id: com.cooreo.sippy.model.AccentId): Accent = when (id) {
    com.cooreo.sippy.model.AccentId.BLUE -> Accent.BLUE
    com.cooreo.sippy.model.AccentId.TEAL -> Accent.TEAL
    com.cooreo.sippy.model.AccentId.VIOLET -> Accent.VIOLET
    com.cooreo.sippy.model.AccentId.CORAL -> Accent.CORAL
}

fun Accent.toId(): com.cooreo.sippy.model.AccentId = when (this) {
    Accent.BLUE -> com.cooreo.sippy.model.AccentId.BLUE
    Accent.TEAL -> com.cooreo.sippy.model.AccentId.TEAL
    Accent.VIOLET -> com.cooreo.sippy.model.AccentId.VIOLET
    Accent.CORAL -> com.cooreo.sippy.model.AccentId.CORAL
}

/** Sippy colour tokens (mirror of shared/tokens.md). */
data class SippyColors(
    val isDark: Boolean,
    val bg0: Color,
    val bg1: Color,
    val bg2: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentBright: Color,
    val accentDeep: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val glassTint: Color,
    val glassRimStart: Color,
    val glassRimEnd: Color,
    val glassInner: Color,
    val scrim: Color,
)

val DarkColors = SippyColors(
    isDark = true,
    bg0 = Color(0xFF05070D),
    bg1 = Color(0xFF0A101C),
    bg2 = Color(0xFF111A2E),
    text1 = Color(0xFFF4F7FF),
    text2 = Color(0xFFA9B4CE),
    text3 = Color(0xFF74819E),
    accent = Color(0xFF2E7CF6),
    accentBright = Color(0xFF5B9BFF),
    accentDeep = Color(0xFF1D5FE0),
    ok = Color(0xFF3ECF8E),
    warn = Color(0xFFF5B84D),
    danger = Color(0xFFF26D6D),
    glassTint = Color(0x14FFFFFF),
    glassRimStart = Color(0x66FFFFFF),
    glassRimEnd = Color(0x2EFFFFFF),
    glassInner = Color(0x1AFFFFFF),
    scrim = Color(0x59000000),
)

val LightColors = SippyColors(
    isDark = false,
    bg0 = Color(0xFFEDF2FB),
    bg1 = Color(0xFFFFFFFF),
    bg2 = Color(0xFFE1E9F8),
    text1 = Color(0xFF0C1424),
    text2 = Color(0xFF43506B),
    text3 = Color(0xFF6C7A97),
    accent = Color(0xFF2E7CF6),
    accentBright = Color(0xFF1D5FE0),
    accentDeep = Color(0xFF1749B8),
    ok = Color(0xFF1FA971),
    warn = Color(0xFFC98A1B),
    danger = Color(0xFFD94F4F),
    glassTint = Color(0x85FFFFFF),
    glassRimStart = Color(0xF2FFFFFF),
    glassRimEnd = Color(0x8CFFFFFF),
    glassInner = Color(0x99FFFFFF),
    scrim = Color(0x80FFFFFF),
)

val LocalSippyColors = staticCompositionLocalOf { DarkColors }

/** Extra shape / spacing tokens that are not part of Material's scheme. */
object Dimens {
    val pill = 999.dp
    val card = 28.dp
    val inner = 20.dp
    val screenPadding = 20.dp
    val gap = 12.dp
    val touch = 48.dp
}

@Composable
fun SippyTheme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.BLUE,
    content: @Composable () -> Unit,
) {
    val base = if (dark) DarkColors else LightColors
    val colors = base.copy(
        accent = accent.seed,
        accentBright = if (dark) accent.bright else accent.seed,
    )
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.bg0,
            onBackground = colors.text1,
            surface = colors.bg1,
            onSurface = colors.text1,
            secondary = colors.accentBright,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accentDeep,
            onPrimary = Color.White,
            background = colors.bg0,
            onBackground = colors.text1,
            surface = colors.bg1,
            onSurface = colors.text1,
            secondary = colors.accentBright,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalSippyColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = SippyTypography, content = content)
    }
}
