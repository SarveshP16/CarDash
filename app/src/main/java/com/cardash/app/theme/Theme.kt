package com.cardash.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.cardash.app.R

/**
 * Minimal palette: a flat background, borderless tiles one step lighter (or white by day),
 * and a single teal accent for the few things that matter (Maps, play, your position).
 */
private val NightColors = darkColorScheme(
    primary = Color(0xFF4FD1C5),
    onPrimary = Color(0xFF00201D),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF8A8A8A),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0000),
)

private val DayColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    background = Color(0xFFF4F4F2),
    onBackground = Color(0xFF141414),
    surface = Color.White,
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFECECEA),
    onSurfaceVariant = Color(0xFF6E6E6E),
    error = Color(0xFFC62828),
    onError = Color.White,
)

/** Heat colours stay the same in both themes so a warning always reads as a warning. */
object HeatColors {
    val Hot = Color(0xFFFFA726)
    val Critical = Color(0xFFFF5252)
}

/** Lexend (bundled, variable weight): designed for fast, easy reading, which suits glances while driving. */
@OptIn(ExperimentalTextApi::class)
val Lexend = FontFamily(
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold).map { weight ->
        Font(R.font.lexend, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

private val LexendTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Lexend),
        displayMedium = t.displayMedium.copy(fontFamily = Lexend),
        displaySmall = t.displaySmall.copy(fontFamily = Lexend),
        headlineLarge = t.headlineLarge.copy(fontFamily = Lexend),
        headlineMedium = t.headlineMedium.copy(fontFamily = Lexend),
        headlineSmall = t.headlineSmall.copy(fontFamily = Lexend),
        titleLarge = t.titleLarge.copy(fontFamily = Lexend),
        titleMedium = t.titleMedium.copy(fontFamily = Lexend),
        titleSmall = t.titleSmall.copy(fontFamily = Lexend),
        bodyLarge = t.bodyLarge.copy(fontFamily = Lexend),
        bodyMedium = t.bodyMedium.copy(fontFamily = Lexend),
        bodySmall = t.bodySmall.copy(fontFamily = Lexend),
        labelLarge = t.labelLarge.copy(fontFamily = Lexend),
        labelMedium = t.labelMedium.copy(fontFamily = Lexend),
        labelSmall = t.labelSmall.copy(fontFamily = Lexend),
    )
}

@Composable
fun CarDashTheme(night: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (night) NightColors else DayColors,
        typography = LexendTypography,
        content = content,
    )
}
