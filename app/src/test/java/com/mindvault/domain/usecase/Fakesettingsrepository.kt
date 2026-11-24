package com.mindvault.domain.usecase

import com.mindvault.domain.model.DeckRemovalPreference
import com.mindvault.domain.model.RecentSearches
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: ThemePreference = ThemePreference.SYSTEM) : SettingsRepository {
    val theme = MutableStateFlow(initial)
    val newCardsPerDay = MutableStateFlow(ReviewQueueBuilder.DEFAULT_NEW_CARDS_PER_DAY)
    val deckRemoval = MutableStateFlow(DeckRemovalPreference.ASK)
    val searchSuggestionsEnabled = MutableStateFlow(true)
    val recentSearches = MutableStateFlow<List<String>>(emptyList())

    override fun observeThemePreference(): Flow<ThemePreference> = theme
    override suspend fun setThemePreference(preference: ThemePreference) {
        theme.value = preference
    }

    override fun observeNewCardsPerDay(): Flow<Int> = newCardsPerDay
    override suspend fun setNewCardsPerDay(limit: Int) {
        newCardsPerDay.value = limit
    }

    override fun observeDeckRemovalPreference(): Flow<DeckRemovalPreference> = deckRemoval
    override suspend fun setDeckRemovalPreference(preference: DeckRemovalPreference) {
        deckRemoval.value = preference
    }

    override fun observeSearchSuggestionsEnabled(): Flow<Boolean> = searchSuggestionsEnabled
    override suspend fun setSearchSuggestionsEnabled(enabled: Boolean) {
        searchSuggestionsEnabled.value = enabled
    }

    override fun observeRecentSearches(): Flow<List<String>> = recentSearches
    override suspend fun addRecentSearch(query: String) {
        recentSearches.value = RecentSearches.add(recentSearches.value, query)
    }

    override suspend fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }
}