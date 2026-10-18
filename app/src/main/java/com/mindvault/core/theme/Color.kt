package com.mindvault.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A supporting accent: a readable foreground plus its background tint. */
@Immutable
data class AccentColors(val foreground: Color, val background: Color)

/** One complete set of MindVault color tokens. There is a light and a dark instance. */
@Immutable
class MindVaultPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val sageLight: Color,
    val sage: Color,
    val sageDeep: Color,
    val forest: Color,
    val forestPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val border: Color,
    val disabled: Color,
    val scrim: Color,
    val accentGold: AccentColors,
    val accentTerracotta: AccentColors,
    val accentBlue: AccentColors,
    val accentLilac: AccentColors,
    val accentMint: AccentColors,
)

/** Light palette: canonical values from DESIGN_SYSTEM.md section 2. */
val LightPalette = MindVaultPalette(
    background = Color(0xFFF0F1E9),
    surface = Color(0xFFFAFAF6),
    surfaceVariant = Color(0xFFE6E9DF),
    sageLight = Color(0xFFD5E0D2),
    sage = Color(0xFF779782),
    sageDeep = Color(0xFF4F765F),
    forest = Color(0xFF286348),
    forestPressed = Color(0xFF1E5039),
    textPrimary = Color(0xFF202820),
    textSecondary = Color(0xFF5F685F),
    textOnPrimary = Color(0xFFFFFFFF),
    border = Color(0xFFD6DCCF),
    disabled = Color(0xFFAEB5AA),
    scrim = Color(0xFF202820).copy(alpha = 0.32f),
    accentGold = AccentColors(Color(0xFF79571B), Color(0xFFF6E8C7)),
    accentTerracotta = AccentColors(Color(0xFF984D3E), Color(0xFFF4DED7)),
    accentBlue = AccentColors(Color(0xFF3559A6), Color(0xFFDFE7FA)),
    accentLilac = AccentColors(Color(0xFF6247A5), Color(0xFFE9E1F8)),
    accentMint = AccentColors(Color(0xFF23694D), Color(0xFFDCEDE2)),
)

/**
 * Dark palette: the same Sage & Stone identity on deep green-tinted stone (not pure black).
 * Forest becomes a lighter green so it still reads as the primary action color, and
 * textOnPrimary becomes dark so text on Forest stays readable. Contrast not yet measured.
 */
val DarkPalette = MindVaultPalette(
    background = Color(0xFF141A16),
    surface = Color(0xFF1D241F),
    surfaceVariant = Color(0xFF2A332C),
    sageLight = Color(0xFF2F4237),
    sage = Color(0xFF8FB09A),
    sageDeep = Color(0xFF6F9B80),
    forest = Color(0xFF7DC4A0),
    forestPressed = Color(0xFF62AD87),
    textPrimary = Color(0xFFE8ECE4),
    textSecondary = Color(0xFFA7B1A6),
    textOnPrimary = Color(0xFF0E2418),
    border = Color(0xFF3A443B),
    disabled = Color(0xFF6B746A),
    scrim = Color(0xFF000000).copy(alpha = 0.5f),
    accentGold = AccentColors(Color(0xFFE6C27A), Color(0xFF3A2E16)),
    accentTerracotta = AccentColors(Color(0xFFE8A596), Color(0xFF3F2621)),
    accentBlue = AccentColors(Color(0xFF9DB7F0), Color(0xFF1E2A47)),
    accentLilac = AccentColors(Color(0xFFBFAEF0), Color(0xFF2E2650)),
    accentMint = AccentColors(Color(0xFF8FD1B0), Color(0xFF1D3A2C)),
)

/** The palette for the current theme. Provided by [MindVaultTheme]. */
val LocalMindVaultPalette = staticCompositionLocalOf { LightPalette }

/**
 * Token access used throughout the UI, e.g. MindVaultColors.Forest. Each property reads the
 * active palette, so screens and components switch between light and dark automatically.
 * These can only be read from composable code.
 */
object MindVaultColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.surfaceVariant
    val SageLight: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.sageLight
    val Sage: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.sage
    val SageDeep: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.sageDeep
    val Forest: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.forest
    val ForestPressed: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.forestPressed
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.textSecondary
    val TextOnPrimary: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.textOnPrimary
    val Border: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.border
    val Disabled: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.disabled
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.scrim

    val AccentGold: AccentColors @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.accentGold
    val AccentTerracotta: AccentColors @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.accentTerracotta
    val AccentBlue: AccentColors @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.accentBlue
    val AccentLilac: AccentColors @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.accentLilac
    val AccentMint: AccentColors @Composable @ReadOnlyComposable get() = LocalMindVaultPalette.current.accentMint
}