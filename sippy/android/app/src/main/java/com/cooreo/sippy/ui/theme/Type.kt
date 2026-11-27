package com.cooreo.sippy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cooreo.sippy.R

/**
 * Inter, instanced at 400 and 700 from the website's own variable font
 * (`sippy/site/public/fonts/inter-latin-var.woff2`, decompressed and pinned into
 * `res/font`). SemiBold/Medium resolve to the nearest available weight (700), so
 * the app and the site share one typeface instead of Roboto.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * The small set of text roles the app uses. Values follow the site's scale:
 * big tight display, 22sp titles, 15sp body, 12sp captions, 11sp tracked labels.
 */
object SippyType {
    val display = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    )
    val title = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp,
    )
    val value = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.4).sp,
    )
    val emphasis = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
    val body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    )
    val caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    )
}

/** Material's roles mapped onto [SippyType] so unstyled Text still uses Inter. */
val SippyTypography = Typography(
    displayLarge = SippyType.display,
    displayMedium = SippyType.value,
    headlineMedium = SippyType.title,
    titleLarge = SippyType.title,
    titleMedium = SippyType.emphasis,
    titleSmall = SippyType.emphasis,
    bodyLarge = SippyType.body,
    bodyMedium = SippyType.body,
    bodySmall = SippyType.caption,
    labelLarge = SippyType.emphasis,
    labelMedium = SippyType.label,
    labelSmall = SippyType.label,
)
