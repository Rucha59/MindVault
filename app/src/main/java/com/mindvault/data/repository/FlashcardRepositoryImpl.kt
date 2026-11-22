package com.mindvault.data.repository

import androidx.room.withTransaction
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import com.mindvault.data.mapper.toDomain
import com.mindvault.data.mapper.toEntity
import com.mindvault.domain.error.DefaultDeckProtectedException
import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val active: ActiveDatabase,
) : FlashcardRepository {

    override fun observeByDeck(deckId: String): Flow<List<FlashcardDetail>> =
        active.observe { db -> db.flashcardDao().observeByDeck(deckId) }
            .map { list -> list.map { it.toDomain() } }

    override fun observeDue(deckId: String, now: Long): Flow<List<FlashcardDetail>> =
        active.observe { db -> db.flashcardDao().observeDue(deckId, now) }
            .map { list -> list.map { it.toDomain() } }

    override fun observeCountByDeck(deckId: String): Flow<Int> =
        active.observe { db -> db.flashcardDao().observeCountByDeck(deckId) }

    override fun observeUnreviewedCountByDeck(deckId: String): Flow<Int> =
        active.observe { db -> db.flashcardDao().observeUnreviewedCountByDeck(deckId) }

    override fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int> =
        active.observe { db -> db.flashcardDao().observeDueCountByDeck(deckId, now) }

    override fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>> =
        active.observe { db -> db.flashcardDao().observeDeckIdsContaining(knowledgeItemId) }

    override suspend fun addToDeck(knowledgeItemId: String, deckId: String): AddToDeckResult {
        // One database for the whole transaction, even if the account changes meanwhile.
        val db = active.current
        val flashcardDao = db.flashcardDao()
        val reviewStateDao = db.reviewStateDao()
        val outbox = db.syncOutboxDao()
        return db.withTransaction {
            val card = FlashcardEntity(
                id = newId(),
                knowledgeItemId = knowledgeItemId,
                deckId = deckId,
                createdAt = nowMillis(),
            )
            if (flashcardDao.insert(card) == -1L) {
                AddToDeckResult.AlreadyInDeck
            } else {
                reviewStateDao.upsert(
                    ReviewState.initial(card.id, card.createdAt).toEntity(updatedAt = card.createdAt),
                )
                outbox.upsert(outboxEntry(SyncEntityType.FLASHCARD, card.id, SyncOperation.UPSERT, card.createdAt))
                outbox.upsert(outboxEntry(SyncEntityType.REVIEW_STATE, card.id, SyncOperation.UPSERT, card.createdAt))
                AddToDeckResult.Added(card.toDomain())
            }
        }
    }

    /**
     * The review state goes with the card (database cascade); only the card's deletion is recorded for sync.
     * Cards of the default deck cannot be removed by hand: they leave only when their Vault item is deleted.
     */
    override suspend fun removeFromDeck(flashcardId: String) {
        val db = active.current
        db.withTransaction {
            if (db.flashcardDao().getById(flashcardId)?.deckId == Deck.DEFAULT_DECK_ID) {
                throw DefaultDeckProtectedException()
            }
            db.flashcardDao().deleteById(flashcardId)
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.FLASHCARD, flashcardId, SyncOperation.DELETE, nowMillis()),
            )
        }
    }

    override suspend fun getReviewState(flashcardId: String): ReviewState? =
        active.current.reviewStateDao().getByFlashcardId(flashcardId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) {
        val db = active.current
        db.withTransaction {
            val dao = db.reviewStateDao()
            val updatedAt = nextUpdatedAt(dao.getByFlashcardId(state.flashcardId)?.updatedAt ?: 0L)
            dao.upsert(state.toEntity(updatedAt = updatedAt))
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.REVIEW_STATE, state.flashcardId, SyncOperation.UPSERT, updatedAt),
            )
        }
    }
}