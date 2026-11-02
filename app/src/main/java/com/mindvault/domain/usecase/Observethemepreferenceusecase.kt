package com.mindvault.domain.usecase

import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveThemePreferenceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemePreference> = repository.observeThemePreference()
}