package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val YusufDarkColorScheme = darkColorScheme(
    primary = ElectricYellow,
    onPrimary = DeepBlack,
    primaryContainer = SoftYellowTint,
    onPrimaryContainer = ElectricYellow,
    secondary = AmberGold,
    onSecondary = DeepBlack,
    secondaryContainer = ElevatedCharcoal,
    onSecondaryContainer = PureWhite,
    tertiary = StatusAccepted,
    onTertiary = DeepBlack,
    background = DeepBlack,
    onBackground = PureWhite,
    surface = DeepBlack,
    onSurface = PureWhite,
    surfaceVariant = CharcoalSurface,
    onSurfaceVariant = LightGray,
    surfaceContainerLowest = DeepBlack,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalSurface,
    surfaceContainerHigh = ElevatedCharcoal,
    surfaceContainerHighest = ColorTokens.ElevatedHigh,
    outline = SubtleBorder,
    outlineVariant = SubtleBorder,
    error = DangerRed,
    onError = PureWhite
)

private object ColorTokens {
    val ElevatedHigh = androidx.compose.ui.graphics.Color(0xFF2B2B2B)
}

val YusufShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YusufElectronicsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YusufDarkColorScheme,
        typography = YusufTypography,
        shapes = YusufShapes,
        content = content
    )
}
