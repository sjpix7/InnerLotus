package com.sjpixels.innerlotus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.R
import com.sjpixels.innerlotus.core.I18n

object Lotus {
    val Plum = Color(0xFF5B3F8F)
    val PlumDeep = Color(0xFF3B2A5E)
    val Ink = Color(0xFF2B2240)
    val InkSoft = Color(0xFF5E5475)
    val Rose = Color(0xFFE58FB1)
    val Teal = Color(0xFF3FA7A3)
    val Gold = Color(0xFFE2B23C)
    val Glass = Color(0xB3FFFFFF)      // translucent white panels
    val GlassStrong = Color(0xE6FFFFFF)
    val Veil = Color(0x33FFFFFF)
    val Danger = Color(0xFFC0392B)
}

val Serif = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.SemiBold, FontStyle.Italic),
)

val Script = FontFamily(Font(R.font.dancing_script, FontWeight.SemiBold))

/** Script face for Latin languages; Cormorant italic for Russian; system serif otherwise. */
fun scriptFamily(): FontFamily = when {
    I18n.lang.scriptFont -> Script
    I18n.lang.code == "ru" -> Serif
    else -> FontFamily.Serif
}

/** Serif display face; for Hindi/CJK the system font renders better than a Latin face. */
fun displayFamily(): FontFamily = if (I18n.lang.scriptFont || I18n.lang.code == "ru") Serif else FontFamily.Default

private val colors = lightColorScheme(
    primary = Lotus.Plum,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Lotus.PlumDeep,
    secondary = Lotus.Rose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EC),
    onSecondaryContainer = Color(0xFF5A2140),
    tertiary = Lotus.Teal,
    background = Color(0xFFF6F1FA),
    onBackground = Lotus.Ink,
    surface = Color(0xFFFBF8FD),
    onSurface = Lotus.Ink,
    surfaceVariant = Color(0xFFEDE6F3),
    onSurfaceVariant = Lotus.InkSoft,
    outline = Color(0xFFB9AEC9),
    error = Lotus.Danger,
)

@Composable
fun InnerLotusTheme(content: @Composable () -> Unit) {
    val display = displayFamily()
    val base = Typography()
    val type = Typography(
        displaySmall = base.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.Bold, color = Lotus.Ink),
        headlineMedium = base.headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}

val CardTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Lotus.Ink)
