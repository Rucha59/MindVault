package com.mindvault.domain.repository

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.KnowledgeType
import kotlinx.coroutines.flow.Flow

interface KnowledgeRepository {
    fun observeAll(): Flow<List<KnowledgeItem>>
    fun observeById(id: String): Flow<KnowledgeItem?>
    suspend fun getById(id: String): KnowledgeItem?

    /** Case-insensitive match; used to flag a Search result that is already saved. */
    suspend fun findByTitle(title: String): KnowledgeItem?

    /** Case-insensitive title AND exact type, so a word and a concept with the same title can both be saved. */
    suspend fun findByTitleAndType(title: String, type: KnowledgeType): KnowledgeItem?

    suspend fun create(draft: KnowledgeItemDraft): KnowledgeItem

    /**
     * Keeps createdAt, sets updatedAt. Returns null if the item no longer exists. An item moved into a category
     * gets a card in that category's deck. If the item leaves a category and [removeFromOldCategoryDeck] is true,
     * its card in that category's deck is removed in the same transaction (cards in other decks stay).
     */
    suspend fun update(item: KnowledgeItem, removeFromOldCategoryDeck: Boolean = false): KnowledgeItem?

    /** Also removes the item's flashcards and review state. */
    suspend fun delete(id: String)
}