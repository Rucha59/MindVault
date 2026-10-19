package com.mindvault.data.repository

import androidx.room.withTransaction
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.dao.FlashcardDao
import com.mindvault.data.local.dao.ReviewStateDao
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.mapper.toDomain
import com.mindvault.data.mapper.toEntity
import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val database: MindVaultDatabase,
    private val flashcardDao: FlashcardDao,
    private val reviewStateDao: ReviewStateDao,
) : FlashcardRepository {

    override fun observeByDeck(deckId: String): Flow<List<FlashcardDetail>> =
        flashcardDao.observeByDeck(deckId).map { list -> list.map { it.toDomain() } }

    override fun observeDue(deckId: String, now: Long): Flow<List<FlashcardDetail>> =
        flashcardDao.observeDue(deckId, now).map { list -> list.map { it.toDomain() } }

    override fun observeCountByDeck(deckId: String): Flow<Int> =
        flashcardDao.observeCountByDeck(deckId)

    override fun observeUnreviewedCountByDeck(deckId: String): Flow<Int> =
        flashcardDao.observeUnreviewedCountByDeck(deckId)

    override fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int> =
        flashcardDao.observeDueCountByDeck(deckId, now)

    override fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>> =
        flashcardDao.observeDeckIdsContaining(knowledgeItemId)

    override suspend fun addToDeck(knowledgeItemId: String, deckId: String): AddToDeckResult =
        database.withTransaction {
            val card = FlashcardEntity(
                id = newId(),
                knowledgeItemId = knowledgeItemId,
                deckId = deckId,
                createdAt = nowMillis(),
            )
            if (flashcardDao.insert(card) == -1L) {
                AddToDeckResult.AlreadyInDeck
            } else {
                reviewStateDao.upsert(ReviewState.initial(card.id, card.createdAt).toEntity())
                AddToDeckResult.Added(card.toDomain())
            }
        }

    override suspend fun removeFromDeck(flashcardId: String) = flashcardDao.deleteById(flashcardId)

    override suspend fun getReviewState(flashcardId: String): ReviewState? =
        reviewStateDao.getByFlashcardId(flashcardId)?.toDomain()

    override suspend fun saveReviewState(state: ReviewState) =
        reviewStateDao.upsert(state.toEntity())
}