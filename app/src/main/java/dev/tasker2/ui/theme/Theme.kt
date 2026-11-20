package dev.tasker2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF28292C)
val CardColor = Color(0xFF1E1F21)
val AccentBlue = Color(0xFFA8C7FA)
val FabBlue = Color(0xFF2350B0)
val Muted = Color(0xFF9AA0A6)
val BadgeBg = Color(0xFF3C3D41)

private val colors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = FabBlue,
    onPrimaryContainer = Color.White,
    background = Background,
    onBackground = Color(0xFFE3E3E3),
    surface = Background,
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = CardColor,
    onSurfaceVariant = Muted,
    surfaceContainer = CardColor,
    surfaceContainerHigh = Color(0xFF2E2F33),
    outline = Color(0xFF5F6368),
)

@Composable
fun TaskerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
