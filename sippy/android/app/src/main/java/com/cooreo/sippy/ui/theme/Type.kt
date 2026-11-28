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
    /** B4: screen titles — 28sp, semibold, tight tracking. */
    val title = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp,
    )
    /**
     * B4: the orb number — 56sp light with tabular figures, so a counting number
     * does not jitter sideways while it climbs.
     */
    val value = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.6).sp,
        fontFeatureSettings = "tnum",
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
    /** B4: small labels — 12sp medium, wide tracking. */
    val label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
    )

    // Round 9, 1.2 — the scale, as a rule rather than a habit:
    //   title 28, section 18, body 15, secondary 13, label 12, tab label 11.
    // Nothing in the app may go below 11sp, and nothing may be set in dp.

    /** Section headings inside a screen or a card. */
    val section = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    )

    /** Secondary copy: footnotes, helper text, the line under a title. */
    val secondary = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** The smallest text the app is allowed to draw: tab labels and badges. */
    val tab = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.3.sp,
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
