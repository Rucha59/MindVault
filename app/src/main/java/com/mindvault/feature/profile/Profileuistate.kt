package com.mindvault.feature.profile

import com.mindvault.domain.model.ThemePreference

data class ProfileUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    /** True when the last attempt to save a choice failed. */
    val saveFailed: Boolean = false,
)