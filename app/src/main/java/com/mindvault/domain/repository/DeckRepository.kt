package com.mindvault.domain.repository

import com.mindvault.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeAll(): Flow<List<Deck>>
    fun observeById(id: String): Flow<Deck?>
    suspend fun getById(id: String): Deck?

    /** @throws com.mindvault.domain.error.DuplicateNameException */
    suspend fun create(name: String): Deck

    /**
     * @throws com.mindvault.domain.error.DuplicateNameException
     * @throws com.mindvault.domain.error.DefaultDeckProtectedException for the default deck.
     */
    suspend fun rename(id: String, name: String): Deck?

    /** @throws com.mindvault.domain.error.DefaultDeckProtectedException for the default deck. */
    suspend fun delete(id: String)

    /**
     * The deck made for a category: returns it, or creates it (named after the category, with a card for each of
     * its items) if the category has none. Null if the category does not exist.
     */
    suspend fun ensureCategoryDeck(categoryId: String): Deck?

    /** The deck made for a category, or null. */
    suspend fun findCategoryDeck(categoryId: String): Deck?

    /** The category's deck, if [itemId] has a card in it; otherwise null. */
    suspend fun findCategoryDeckHoldingItem(itemId: String, categoryId: String): Deck?
}