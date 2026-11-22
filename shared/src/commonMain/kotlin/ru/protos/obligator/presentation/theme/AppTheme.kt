package ru.protos.obligator.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomColors(
    val highlightBackground: Color,
    val highlightText: Color
)

private val LightCustomColors = CustomColors(
    highlightBackground = Color(0xFFE8F5E9),
    highlightText = Color(0xFF1B5E20)
)

private val DarkCustomColors = CustomColors(
    highlightBackground = Color(0xFF1B5E20),
    highlightText = Color(0xFFA5D6A7)
)

val LocalCustomColors = staticCompositionLocalOf { LightCustomColors }

private val LightColorPalette = lightColors(
    primary = Color(0xFF1976D2),
    primaryVariant = Color(0xFF0D47A1),
    secondary = Color(0xFF00796B)
)

private val DarkColorPalette = darkColors(
    primary = Color(0xFF90CAF9),
    primaryVariant = Color(0xFF1976D2),
    secondary = Color(0xFF80CBC4)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colors = colors,
            content = content
        )
    }
}

object AppTheme {
    val customColors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
