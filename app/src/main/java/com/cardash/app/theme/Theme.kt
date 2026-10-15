package com.cardash.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF4FD1C5)
private val TealDark = Color(0xFF0F766E)

// Night: near-black so the screen doesn't glare at the windscreen.
private val NightColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201D),
    secondary = Color(0xFF9AA4AE),
    background = Color(0xFF07090B),
    onBackground = Color(0xFFE6EAED),
    surface = Color(0xFF12161A),
    onSurface = Color(0xFFE6EAED),
    surfaceVariant = Color(0xFF1C2228),
    onSurfaceVariant = Color(0xFF9AA4AE),
    error = Color(0xFFFF6B6B),
)

private val DayColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    secondary = Color(0xFF52606D),
    background = Color(0xFFEEF1F3),
    onBackground = Color(0xFF101418),
    surface = Color.White,
    onSurface = Color(0xFF101418),
    surfaceVariant = Color(0xFFE1E6EA),
    onSurfaceVariant = Color(0xFF52606D),
    error = Color(0xFFC62828),
)

/** Heat colours stay the same in both themes so a warning always reads as a warning. */
object HeatColors {
    val Hot = Color(0xFFFFA726)
    val Critical = Color(0xFFFF5252)
}

@Composable
fun CarDashTheme(night: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (night) NightColors else DayColors, content = content)
}
