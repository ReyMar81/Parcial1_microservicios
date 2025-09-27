package com.tiendavirtual.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores inspirados en la interfaz moderna de las imágenes
private val Purple80 = Color(0xFF8E44AD)
private val PurpleGrey80 = Color(0xFF9B59B6)
private val Pink80 = Color(0xFFE91E63)

private val Purple40 = Color(0xFF663399)
private val PurpleGrey40 = Color(0xFF7B1FA2)
private val Pink40 = Color(0xFFAD1457)

// Colores adicionales para la interfaz
val GreenSuccess = Color(0xFF4CAF50)
val RedError = Color(0xFFE53E3E)
val OrangeWarning = Color(0xFFFF9800)
val BlueInfo = Color(0xFF2196F3)
val LightGray = Color(0xFFF5F5F5)
val DarkGray = Color(0xFF424242)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF2D2D2D),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun TiendaVirtualTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
