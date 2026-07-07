package com.varidian.varidiansms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo200,
    onPrimary = Indigo900,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = IndigoContainerLight,
    secondary = Teal200,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = TealContainerLight,
    tertiary = Violet200,
    background = SurfaceDark,
    surface = SurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Indigo900,
    secondary = Teal600,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Color(0xFF134E48),
    tertiary = Violet500,
    background = SurfaceLight,
    surface = SurfaceLight,
)

/** Status colors that adapt to the active theme. */
object StatusColors {
    val green: Color @Composable get() = if (isSystemInDarkTheme()) StatusGreenDark else StatusGreen
    val amber: Color @Composable get() = if (isSystemInDarkTheme()) StatusAmberDark else StatusAmber
    val red: Color @Composable get() = if (isSystemInDarkTheme()) StatusRedDark else StatusRed
    val blue: Color @Composable get() = if (isSystemInDarkTheme()) StatusBlueDark else StatusBlue
}

@Composable
fun VaridianSMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
