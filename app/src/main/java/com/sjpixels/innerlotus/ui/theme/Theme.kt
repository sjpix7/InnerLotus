package com.sjpixels.innerlotus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.R
import com.sjpixels.innerlotus.core.I18n

/**
 * Inner Lotus palette — "glass lotus at dawn". Ink + muted plum text on pastel glass
 * (blush, lavender, sage, peach, cream), one soft violet accent. See docs/design/DESIGN_SYSTEM.md.
 */
object Lotus {
    // Text & ink
    val Ink = Color(0xFF302C43)          // headings, body, primary pill buttons
    val InkSoft = Color(0xFF574F6A)      // muted text (≥ 5:1 on the dawn sky and on glass)
    val InkFaint = Color(0xFF8F88A0)     // captions on strong glass only
    val Accent = Color(0xFF7760A9)       // links, active icons, selection
    val AccentSoft = Color(0xFF9278B7)   // switches, sliders, progress
    val Line = Color(0x2161497B)         // hairline dividers

    // Pastel tints (from the signature image and logo)
    val Blush = Color(0xFFF6D4E6)
    val Lavender = Color(0xFFE6DFF7)
    val Sage = Color(0xFFDEEBE3)
    val Peach = Color(0xFFFAE3D7)
    val Cream = Color(0xFFFCF8F5)
    val Mauve = Color(0xFFCDAABE)
    val BadgeBg = Color(0xFFE9E1F4)      // icon badge squares
    val BadgeInk = Color(0xFF7D619C)
    val Sheet = Color(0xFFF8F3FA)        // dialogs / sheets

    // Soft semantic colours
    val Rose = Color(0xFFC686AB)         // favourites, gentle emphasis
    val SageInk = Color(0xFF5E8A6B)      // positive / done
    val Gold = Color(0xFFC9A55A)
    val Danger = Color(0xFFB14F63)       // destructive / crisis (soft, not alarm-red)

    // Legacy names (kept so older call sites compile; mapped to the new palette)
    val Plum = Accent
    val PlumDeep = Ink
    val Teal = SageInk
    val Glass = Color(0x94FFFFFF)
    val GlassStrong = Color(0xDDFFFFFF)
    val Veil = Color(0x26FFFFFF)
}

/** Variable-font instance at an exact weight (API 26+ applies the 'wght' axis). */
@OptIn(ExperimentalTextApi::class)
private fun vf(res: Int, weight: Int, italic: Boolean = false) = Font(
    res,
    FontWeight(weight),
    if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Cormorant Garamond — display serif (headlines, affirmation text), upright + italic, 300–700. */
val Cormorant = FontFamily(
    vf(R.font.cormorant_garamond, 300), vf(R.font.cormorant_garamond, 400), vf(R.font.cormorant_garamond, 500),
    vf(R.font.cormorant_garamond, 600), vf(R.font.cormorant_garamond, 700),
    vf(R.font.cormorant_garamond_italic, 300, true), vf(R.font.cormorant_garamond_italic, 400, true),
    vf(R.font.cormorant_garamond_italic, 500, true), vf(R.font.cormorant_garamond_italic, 600, true),
    vf(R.font.cormorant_garamond_italic, 700, true),
)

/** Jost — airy geometric sans for UI and body text, 300–600. */
val Jost = FontFamily(
    vf(R.font.jost, 300), vf(R.font.jost, 400), vf(R.font.jost, 500), vf(R.font.jost, 600),
)

// Older names, kept for compatibility.
val Serif = Cormorant
val Script = Cormorant

/** Cormorant defaults to old-style figures ("0" looks like "o", "1" like "I"); use lining figures. */
const val LINING = "lnum"

/** Our bundled faces cover Latin and Cyrillic; Hindi and CJK use the system font. */
private fun bundledFacesFit(): Boolean = I18n.lang.scriptFont || I18n.lang.code == "ru"

/** Serif display face (headlines, quotes, numbers). */
fun displayFamily(): FontFamily = if (bundledFacesFit()) Cormorant else FontFamily.Default

/** UI / body face. */
fun bodyFamily(): FontFamily = if (bundledFacesFit()) Jost else FontFamily.Default

/** Italic line on affirmation cards (formerly a handwriting face): Cormorant italic. */
fun scriptFamily(): FontFamily = if (bundledFacesFit()) Cormorant else FontFamily.Serif

/**
 * Extra text roles beyond Material's scale. Sizes are tuned for Cormorant's small x-height
 * (serif sizes run ~15% larger than an equivalent sans).
 */
object LotusType {
    /** Tiny letter-spaced caps line above headlines ("YOUR SPACE TO SIMPLY BE"). Uppercase in code. */
    val kicker @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.16.em, lineHeight = 15.sp, color = Lotus.InkSoft)
    /** Top-bar screen title. */
    val barTitle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, color = Lotus.Ink)
    /** Section heading inside a page ("How are you arriving?"). */
    val section @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 28.sp, color = Lotus.Ink)
    /** Card heading in serif ("A gentler inner voice"). */
    val cardTitle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp, color = Lotus.Ink)
    /** Tile / list-row title (sans). */
    val itemTitle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, color = Lotus.Ink)
    /** Tile / list-row caption. */
    val itemSub @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp, color = Lotus.InkSoft)
    /** Small muted body ("Choose the part of your life…"). */
    val small @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = Lotus.InkSoft)
    /** Centred quiet note / footnote. */
    val micro @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.02.em, color = Lotus.InkSoft)
    /** Quotes and prayer lines. */
    val quote @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.Medium, fontSize = 25.sp, lineHeight = 34.sp, color = Lotus.Ink)
    /** Large light numbers (breath counter, intensity). */
    val bigNumber @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.Normal, fontSize = 68.sp, lineHeight = 72.sp, color = Lotus.Ink)
    /** Stat tiles ("71%"). */
    val stat @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontFeatureSettings = LINING, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, color = Lotus.Ink)
    /** Buttons and pills. */
    val button @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.01.em)
    val pill @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 18.sp)
}

private val colors = lightColorScheme(
    primary = Lotus.Accent,
    onPrimary = Color.White,
    primaryContainer = Lotus.Lavender,
    onPrimaryContainer = Lotus.Ink,
    secondary = Lotus.AccentSoft,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E9F6),
    onSecondaryContainer = Lotus.Ink,
    tertiary = Lotus.SageInk,
    tertiaryContainer = Lotus.Sage,
    background = Lotus.Cream,
    onBackground = Lotus.Ink,
    surface = Lotus.Sheet,
    onSurface = Lotus.Ink,
    surfaceVariant = Color(0xFFEDE6F3),
    onSurfaceVariant = Lotus.InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8FC),
    surfaceContainer = Lotus.Sheet,
    surfaceContainerHigh = Lotus.Sheet,
    surfaceContainerHighest = Color(0xFFF1EBF5),
    outline = Color(0xFFD5CBE0),
    outlineVariant = Lotus.Line,
    error = Lotus.Danger,
)

@Composable
fun InnerLotusTheme(content: @Composable () -> Unit) {
    val display = displayFamily()
    val body = bodyFamily()
    val base = Typography()
    fun TextStyle.d(w: FontWeight, size: Int, lh: Int, ls: Double = 0.0) =
        copy(fontFamily = display, fontWeight = w, fontSize = size.sp, lineHeight = lh.sp, letterSpacing = ls.sp, color = Lotus.Ink, fontFeatureSettings = LINING)
    fun TextStyle.b(w: FontWeight, size: Double, lh: Int, ls: Double = 0.0) =
        copy(fontFamily = body, fontWeight = w, fontSize = size.sp, lineHeight = lh.sp, letterSpacing = ls.sp)
    val type = Typography(
        displayLarge = base.displayLarge.d(FontWeight.Medium, 48, 52, -0.5),
        displayMedium = base.displayMedium.d(FontWeight.Medium, 42, 46, -0.5),
        displaySmall = base.displaySmall.d(FontWeight.Medium, 38, 42, -0.4),   // welcome headline
        headlineLarge = base.headlineLarge.d(FontWeight.Medium, 34, 38, -0.3),  // screen headline
        headlineMedium = base.headlineMedium.d(FontWeight.Medium, 30, 35, -0.2),
        headlineSmall = base.headlineSmall.d(FontWeight.Medium, 26, 31, -0.1),
        titleLarge = base.titleLarge.d(FontWeight.SemiBold, 23, 28),            // section headings
        titleMedium = base.titleMedium.b(FontWeight.Medium, 16.0, 22),          // card / row titles
        titleSmall = base.titleSmall.b(FontWeight.Medium, 14.0, 20),
        bodyLarge = base.bodyLarge.b(FontWeight.Normal, 16.0, 25),
        bodyMedium = base.bodyMedium.b(FontWeight.Normal, 15.0, 22),
        bodySmall = base.bodySmall.b(FontWeight.Normal, 13.0, 19),
        labelLarge = base.labelLarge.b(FontWeight.Medium, 15.0, 20, 0.1),
        labelMedium = base.labelMedium.b(FontWeight.Medium, 12.0, 16, 0.4),
        labelSmall = base.labelSmall.b(FontWeight.Normal, 11.0, 15, 0.3),
    )
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}

val CardTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Lotus.Ink)
