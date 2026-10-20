package com.cardash.link.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.cardash.link.R

/** Lexend, like CarDash; colours follow the phone's wallpaper (Material You). */
@OptIn(ExperimentalTextApi::class)
private val Lexend = FontFamily(
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
fun LinkTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        typography = LexendTypography,
        content = content,
    )
}
