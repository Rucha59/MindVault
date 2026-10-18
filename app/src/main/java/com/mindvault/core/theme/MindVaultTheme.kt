package com.mindvault.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/** Maps a MindVault palette onto the Material 3 color roles. */
private fun MindVaultPalette.toColorScheme(dark: Boolean): ColorScheme {
    // Start from Material defaults so unused roles (e.g. error) have sensible light/dark values.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = forest,
        onPrimary = textOnPrimary,
        primaryContainer = sageLight,
        onPrimaryContainer = textPrimary,
        secondary = sageDeep,
        onSecondary = textOnPrimary,
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = textPrimary,
        tertiary = sageDeep,
        onTertiary = textOnPrimary,
        tertiaryContainer = sageLight,
        onTertiaryContainer = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = border,
        outlineVariant = border,
        scrim = scrim,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
    )
}

/** Light or dark MindVault theme. Follows the system setting unless [darkTheme] is passed. */
@Composable
fun MindVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = remember(darkTheme) { palette.toColorScheme(darkTheme) }

    CompositionLocalProvider(LocalMindVaultPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MindVaultTypography,
            shapes = MindVaultShapes,
            content = content,
        )
    }
}