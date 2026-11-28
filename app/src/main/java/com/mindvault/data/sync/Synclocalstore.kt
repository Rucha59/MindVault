package com.mindvault.data.sync

import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewLogEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.entity.SyncOutboxEntity

/**
 * Everything the sync engine needs from one vault. [RoomSyncLocalStore] is the real one; unit tests use an
 * in-memory fake. Writes made through this interface never touch the outbox on their own: pulled changes must
 * not be uploaded again.
 */
interface SyncLocalStore {
    suspend fun <T> inTransaction(block: suspend () -> T): T

    // Outbox
    suspend fun outboxAll(): List<SyncOutboxEntity>
    suspend fun outboxGet(type: String, id: String): SyncOutboxEntity?
    suspend fun outboxPut(entry: SyncOutboxEntity)
    suspend fun outboxPutIfAbsent(entry: SyncOutboxEntity)
    suspend fun outboxRemove(type: String, id: String)
    suspend fun outboxRemoveIfUnchanged(entry: SyncOutboxEntity)

    // Sync bookkeeping
    suspend fun stateGet(key: String): String?
    suspend fun statePut(key: String, value: String)
    suspend fun stateRemove(key: String)

    // Categories
    suspend fun categories(): List<CategoryEntity>
    suspend fun category(id: String): CategoryEntity?
    suspend fun categoryByName(name: String): CategoryEntity?
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(id: String)

    // Decks
    suspend fun decks(): List<DeckEntity>
    suspend fun deck(id: String): DeckEntity?
    suspend fun deckByName(name: String): DeckEntity?
    suspend fun insertDeck(deck: DeckEntity)
    suspend fun updateDeck(deck: DeckEntity)
    suspend fun deleteDeck(id: String)

    // Knowledge items
    suspend fun items(): List<KnowledgeItemEntity>
    suspend fun item(id: String): KnowledgeItemEntity?
    suspend fun itemsInCategory(categoryId: String): List<KnowledgeItemEntity>
    suspend fun insertItem(item: KnowledgeItemEntity)
    suspend fun updateItem(item: KnowledgeItemEntity)
    suspend fun deleteItem(id: String)

    // Flashcards
    suspend fun flashcards(): List<FlashcardEntity>
    suspend fun flashcard(id: String): FlashcardEntity?
    suspend fun flashcardFor(knowledgeItemId: String, deckId: String): FlashcardEntity?

    /** False when the card (or its item+deck pair) already exists. */
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Boolean
    suspend fun deleteFlashcard(id: String)

    // Review states
    suspend fun reviewStates(): List<ReviewStateEntity>
    suspend fun reviewState(flashcardId: String): ReviewStateEntity?
    suspend fun putReviewState(state: ReviewStateEntity)

    // Review log (append-only history)
    suspend fun reviewLog(id: String): ReviewLogEntity?

    /** False when an entry with this ID already exists. */
    suspend fun insertReviewLog(entry: ReviewLogEntity): Boolean

    /** A never-reviewed state for a card that arrived from the cloud without one. updatedAt must be 0. */
    fun initialReviewState(flashcardId: String, createdAt: Long): ReviewStateEntity
}