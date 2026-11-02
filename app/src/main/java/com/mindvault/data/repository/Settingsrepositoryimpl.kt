package com.mindvault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.repository.SettingsRepository
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

    override fun observeThemePreference(): Flow<ThemePreference> =
        context.settingsDataStore.data
            .catch { error ->
                // An unreadable file falls back to defaults; any other failure is not swallowed.
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { prefs ->
                // Unknown or missing values fall back to SYSTEM.
                ThemePreference.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: ThemePreference.SYSTEM
            }
            .distinctUntilChanged()

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.settingsDataStore.edit { it[THEME_KEY] = preference.name }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
    }
}