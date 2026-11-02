package com.mindvault.domain.usecase

import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemePreferenceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(preference: ThemePreference) =
        repository.setThemePreference(preference)
}