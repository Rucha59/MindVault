package com.mindvault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindvault.domain.model.DeckRemovalPreference
import com.mindvault.domain.model.RecentSearches
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
import com.mindvault.domain.usecase.ReviewQueueBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mindvault_settings")

/** App preferences stored in Preferences DataStore. Saved knowledge and review state stay in Room. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val preferences: Flow<Preferences> = context.settingsDataStore.data
        .catch { error ->
            // An unreadable file falls back to defaults; any other failure is not swallowed.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    override fun observeThemePreference(): Flow<ThemePreference> =
        preferences
            .map { prefs ->
                // Unknown or missing values fall back to SYSTEM.
                ThemePreference.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: ThemePreference.SYSTEM
            }
            .distinctUntilChanged()

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.settingsDataStore.edit { it[THEME_KEY] = preference.name }
    }

    override fun observeNewCardsPerDay(): Flow<Int> =
        preferences
            .map { prefs ->
                (prefs[NEW_CARDS_KEY] ?: ReviewQueueBuilder.DEFAULT_NEW_CARDS_PER_DAY)
                    .coerceIn(ReviewQueueBuilder.NEW_CARDS_PER_DAY_RANGE)
            }
            .distinctUntilChanged()

    override suspend fun setNewCardsPerDay(limit: Int) {
        context.settingsDataStore.edit {
            it[NEW_CARDS_KEY] = limit.coerceIn(ReviewQueueBuilder.NEW_CARDS_PER_DAY_RANGE)
        }
    }

    override fun observeDeckRemovalPreference(): Flow<DeckRemovalPreference> =
        preferences
            .map { prefs ->
                // Unknown or missing values fall back to ASK.
                DeckRemovalPreference.entries.firstOrNull { it.name == prefs[DECK_REMOVAL_KEY] }
                    ?: DeckRemovalPreference.ASK
            }
            .distinctUntilChanged()

    override suspend fun setDeckRemovalPreference(preference: DeckRemovalPreference) {
        context.settingsDataStore.edit { it[DECK_REMOVAL_KEY] = preference.name }
    }

    override fun observeSearchSuggestionsEnabled(): Flow<Boolean> =
        preferences.map { prefs -> prefs[SEARCH_SUGGESTIONS_KEY] ?: true }.distinctUntilChanged()

    override suspend fun setSearchSuggestionsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SEARCH_SUGGESTIONS_KEY] = enabled }
    }

    override fun observeRecentSearches(): Flow<List<String>> =
        preferences.map { prefs -> decodeRecentSearches(prefs[RECENT_SEARCHES_KEY]) }.distinctUntilChanged()

    override suspend fun addRecentSearch(query: String) {
        context.settingsDataStore.edit { prefs ->
            val updated = RecentSearches.add(decodeRecentSearches(prefs[RECENT_SEARCHES_KEY]), query)
            prefs[RECENT_SEARCHES_KEY] = updated.joinToString(RECENT_SEPARATOR)
        }
    }

    override suspend fun clearRecentSearches() {
        context.settingsDataStore.edit { it.remove(RECENT_SEARCHES_KEY) }
    }

    /** Stored as one text, one search per line (searches never contain line breaks: whitespace is collapsed). */
    private fun decodeRecentSearches(stored: String?): List<String> =
        stored.orEmpty().split(RECENT_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.take(RecentSearches.MAX)

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val NEW_CARDS_KEY = intPreferencesKey("new_cards_per_day")
        val DECK_REMOVAL_KEY = stringPreferencesKey("deck_removal_preference")
        val SEARCH_SUGGESTIONS_KEY = booleanPreferencesKey("search_suggestions_enabled")
        val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        const val RECENT_SEPARATOR = "\n"
    }
}