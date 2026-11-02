package com.mindvault.domain.usecase

import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: ThemePreference = ThemePreference.SYSTEM) : SettingsRepository {
    val theme = MutableStateFlow(initial)

    override fun observeThemePreference(): Flow<ThemePreference> = theme
    override suspend fun setThemePreference(preference: ThemePreference) {
        theme.value = preference
    }
}