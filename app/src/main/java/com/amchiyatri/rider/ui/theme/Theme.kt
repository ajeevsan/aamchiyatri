package com.amchiyatri.rider.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberPrimaryContainer,
    secondary = TealAction,
    secondaryContainer = TealActionContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = AmberPrimaryContainer,
    onPrimary = InkBackground,
    primaryContainer = AmberPrimary,
    secondary = TealActionContainer,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = ErrorRed,
)

/**
 * Brand colours stay fixed regardless of wallpaper (no Android 12+ dynamic color) so the app
 * keeps a consistent identity, the same way most ride-hailing apps do.
 */
@Composable
fun AmchiYatriTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AmchiYatriTypography,
        content = content,
    )
}
