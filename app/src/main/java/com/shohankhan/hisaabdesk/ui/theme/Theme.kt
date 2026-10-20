package com.shohankhan.hisaabdesk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGreenDark,
    onSecondary = SurfaceWhite,
    secondaryContainer = BrandGreenLight,
    onSecondaryContainer = BrandGreenDark,
    tertiary = WarningAmber,
    onTertiary = SurfaceWhite,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BrandGreenLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderSoft,
    outlineVariant = BorderSoft,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceWhite,
    inversePrimary = BrandGreenLight
)

/**
 * HisaabDesk ships a single light theme — dark theme is intentionally not provided
 * (product requirement). [darkTheme] is ignored so a system dark setting cannot
 * break the brand palette.
 */
@Composable
fun HisaabDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
