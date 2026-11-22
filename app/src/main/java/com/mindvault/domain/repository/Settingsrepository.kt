package com.mindvault.domain.repository

import com.mindvault.domain.model.DeckRemovalPreference
import com.mindvault.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits the saved theme preference; SYSTEM when nothing is saved yet. */
    fun observeThemePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)

    /** New flashcards introduced per deck per day; 20 when nothing is saved. */
    fun observeNewCardsPerDay(): Flow<Int>

    suspend fun setNewCardsPerDay(limit: Int)

    /** What to do with an item's card in its old category's deck when it leaves the category; ASK when not saved. */
    fun observeDeckRemovalPreference(): Flow<DeckRemovalPreference>

    suspend fun setDeckRemovalPreference(preference: DeckRemovalPreference)
}