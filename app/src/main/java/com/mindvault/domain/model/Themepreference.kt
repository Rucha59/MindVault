package com.mindvault.domain.model

/** The user's light/dark choice. SYSTEM follows the device setting. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    /** Whether the dark theme should be used, given the device's current setting. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }
}