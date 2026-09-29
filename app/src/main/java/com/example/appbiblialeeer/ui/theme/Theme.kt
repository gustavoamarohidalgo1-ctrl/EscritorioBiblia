package com.example.appbiblialeeer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ----------------------------
// 🎨 COLORES (la app siempre usa modo oscuro)
// ----------------------------

private val BibliaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF81D4FA),
    onSecondary = Color.Black,

    background = Color.Black,
    onBackground = Color.White,

    surface = Color(0xFF121212),
    onSurface = Color.White,

    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

// ----------------------------
// 🎨 THEME PRINCIPAL
// ----------------------------

@Composable
fun AppBibliaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BibliaDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
