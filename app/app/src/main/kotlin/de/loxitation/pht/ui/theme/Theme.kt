package de.loxitation.pht.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PhtGreen = Color(0xFF1B5E20)
private val PhtGreenLight = Color(0xFF4C8C4A)
private val PhtError = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = PhtGreen,
    secondary = PhtGreenLight,
    error = PhtError,
)

private val DarkColors = darkColorScheme(
    primary = PhtGreenLight,
    secondary = PhtGreen,
    error = PhtError,
)

@Composable
fun PhtTheme(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
