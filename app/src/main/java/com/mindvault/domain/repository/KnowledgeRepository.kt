package com.mindvault.domain.repository

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import kotlinx.coroutines.flow.Flow

interface KnowledgeRepository {
    fun observeAll(): Flow<List<KnowledgeItem>>
    fun observeById(id: String): Flow<KnowledgeItem?>
    suspend fun getById(id: String): KnowledgeItem?

    /** Case-insensitive match; used to flag a Search result that is already saved. */
    suspend fun findByTitle(title: String): KnowledgeItem?

    suspend fun create(draft: KnowledgeItemDraft): KnowledgeItem

    /** Keeps createdAt, sets updatedAt. Returns null if the item no longer exists. */
    suspend fun update(item: KnowledgeItem): KnowledgeItem?

    /** Also removes the item's flashcards and review state. */
    suspend fun delete(id: String)
}