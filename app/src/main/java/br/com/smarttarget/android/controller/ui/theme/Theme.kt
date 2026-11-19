package br.com.smarttarget.android.controller.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SmartiesDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF042130),
    primaryContainer = ElectricCyanDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = IndigoAccent,
    onSecondary = Color(0xFF141633),
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = EmeraldConnected,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = GraphiteBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = GraphiteBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateElevated,
    surfaceContainerHigh = SlateHighest,
    outline = SubtleOutline,
    outlineVariant = SubtleOutlineBright,
    error = AlertCoral,
    onError = Color(0xFF450A0A),
    errorContainer = AlertCoralContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val SmartiesShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartiesControllerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartiesDarkColorScheme,
        typography = AppTypography,
        shapes = SmartiesShapes,
        content = content
    )
}
