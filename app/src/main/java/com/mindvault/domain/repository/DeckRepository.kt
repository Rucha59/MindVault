package com.mindvault.domain.repository

import com.mindvault.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeAll(): Flow<List<Deck>>
    fun observeById(id: String): Flow<Deck?>
    suspend fun getById(id: String): Deck?

    /** @throws com.mindvault.domain.error.DuplicateNameException */
    suspend fun create(name: String): Deck

    /** @throws com.mindvault.domain.error.DuplicateNameException */
    suspend fun rename(id: String, name: String): Deck?

    /** @throws com.mindvault.domain.error.DefaultDeckProtectedException for the default deck. */
    suspend fun delete(id: String)
}