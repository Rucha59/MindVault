package com.mindvault.data.sync

import androidx.room.withTransaction
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewLogEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.entity.SyncOutboxEntity
import com.mindvault.data.local.entity.SyncStateEntity
import com.mindvault.data.mapper.toEntity
import com.mindvault.domain.model.ReviewState

/** [SyncLocalStore] on one open vault. Create a new one per sync run, from a database read once. */
class RoomSyncLocalStore(private val db: MindVaultDatabase) : SyncLocalStore {

    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction { block() }

    override suspend fun outboxAll(): List<SyncOutboxEntity> = db.syncOutboxDao().getAll()
    override suspend fun outboxGet(type: String, id: String): SyncOutboxEntity? = db.syncOutboxDao().get(type, id)
    override suspend fun outboxPut(entry: SyncOutboxEntity) = db.syncOutboxDao().upsert(entry)
    override suspend fun outboxPutIfAbsent(entry: SyncOutboxEntity) {
        db.syncOutboxDao().insertIfAbsent(entry)
    }
    override suspend fun outboxRemove(type: String, id: String) = db.syncOutboxDao().delete(type, id)
    override suspend fun outboxRemoveIfUnchanged(entry: SyncOutboxEntity) =
        db.syncOutboxDao().deleteIfUnchanged(entry.entityType, entry.entityId, entry.operation, entry.changedAt)

    override suspend fun stateGet(key: String): String? = db.syncStateDao().get(key)
    override suspend fun statePut(key: String, value: String) = db.syncStateDao().put(SyncStateEntity(key, value))
    override suspend fun stateRemove(key: String) = db.syncStateDao().delete(key)

    override suspend fun categories(): List<CategoryEntity> = db.categoryDao().getAll()
    override suspend fun category(id: String): CategoryEntity? = db.categoryDao().getById(id)
    override suspend fun categoryByName(name: String): CategoryEntity? = db.categoryDao().findByName(name)
    override suspend fun insertCategory(category: CategoryEntity) = db.categoryDao().insert(category)
    override suspend fun updateCategory(category: CategoryEntity) {
        db.categoryDao().update(category)
    }
    override suspend fun deleteCategory(id: String) = db.categoryDao().deleteById(id)

    override suspend fun decks(): List<DeckEntity> = db.deckDao().getAll()
    override suspend fun deck(id: String): DeckEntity? = db.deckDao().getById(id)
    override suspend fun deckByName(name: String): DeckEntity? = db.deckDao().findByName(name)
    override suspend fun insertDeck(deck: DeckEntity) = db.deckDao().insert(deck)
    override suspend fun updateDeck(deck: DeckEntity) {
        db.deckDao().update(deck)
    }
    override suspend fun deleteDeck(id: String) {
        db.deckDao().deleteById(id)
    }

    override suspend fun items(): List<KnowledgeItemEntity> = db.knowledgeItemDao().getAll()
    override suspend fun item(id: String): KnowledgeItemEntity? = db.knowledgeItemDao().getById(id)
    override suspend fun itemsInCategory(categoryId: String): List<KnowledgeItemEntity> =
        db.knowledgeItemDao().getByCategory(categoryId)
    override suspend fun insertItem(item: KnowledgeItemEntity) = db.knowledgeItemDao().insert(item)
    override suspend fun updateItem(item: KnowledgeItemEntity) {
        db.knowledgeItemDao().update(item)
    }
    override suspend fun deleteItem(id: String) = db.knowledgeItemDao().deleteById(id)

    override suspend fun flashcards(): List<FlashcardEntity> = db.flashcardDao().getAll()
    override suspend fun flashcard(id: String): FlashcardEntity? = db.flashcardDao().getById(id)
    override suspend fun flashcardFor(knowledgeItemId: String, deckId: String): FlashcardEntity? =
        db.flashcardDao().findByItemAndDeck(knowledgeItemId, deckId)
    override suspend fun insertFlashcard(flashcard: FlashcardEntity): Boolean =
        db.flashcardDao().insert(flashcard) != -1L
    override suspend fun deleteFlashcard(id: String) = db.flashcardDao().deleteById(id)

    override suspend fun reviewStates(): List<ReviewStateEntity> = db.reviewStateDao().getAll()
    override suspend fun reviewState(flashcardId: String): ReviewStateEntity? =
        db.reviewStateDao().getByFlashcardId(flashcardId)
    override suspend fun putReviewState(state: ReviewStateEntity) = db.reviewStateDao().upsert(state)

    override suspend fun reviewLog(id: String): ReviewLogEntity? = db.reviewLogDao().getById(id)
    override suspend fun insertReviewLog(entry: ReviewLogEntity): Boolean = db.reviewLogDao().insert(entry) != -1L

    override fun initialReviewState(flashcardId: String, createdAt: Long): ReviewStateEntity =
        ReviewState.initial(flashcardId, createdAt).toEntity(updatedAt = 0L)
}