package com.mindvault.domain.usecase

import com.mindvault.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether Search shows autocomplete suggestions while typing. On unless the user turned it off. */
class ObserveSearchSuggestionsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeSearchSuggestionsEnabled()
}

class SetSearchSuggestionsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSearchSuggestionsEnabled(enabled)
}

/** The last few searches on this device, newest first. */
class ObserveRecentSearchesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<List<String>> = repository.observeRecentSearches()
}

class AddRecentSearchUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(query: String) = repository.addRecentSearch(query)
}

class ClearRecentSearchesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke() = repository.clearRecentSearches()
}