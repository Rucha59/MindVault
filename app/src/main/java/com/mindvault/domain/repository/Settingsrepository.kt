package com.mindvault.domain.repository

import com.mindvault.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits the saved theme preference; SYSTEM when nothing is saved yet. */
    fun observeThemePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}