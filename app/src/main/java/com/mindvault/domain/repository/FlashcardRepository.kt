package com.mindvault.domain.repository

import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeByDeck(deckId: String): Flow<List<FlashcardDetail>>
    fun observeDue(deckId: String, now: Long): Flow<List<FlashcardDetail>>
    fun observeCountByDeck(deckId: String): Flow<Int>
    fun observeUnreviewedCountByDeck(deckId: String): Flow<Int>
    fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int>

    /** Decks that already contain this item, for the "choose a deck" step. */
    fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>>

    /** Creates the card and its initial review state in one transaction. */
    suspend fun addToDeck(knowledgeItemId: String, deckId: String): AddToDeckResult
    suspend fun removeFromDeck(flashcardId: String)

    suspend fun getReviewState(flashcardId: String): ReviewState?
    suspend fun saveReviewState(state: ReviewState)

    /**
     * Saves [state] after a Hard, Good or Easy answer and logs that answer (review history), in one transaction,
     * both queued for sync. Again is never passed here (it is not logged). [intervalDaysBefore] is the card's
     * interval before the answer. The default only saves the state, so simple fakes keep working;
     * [FlashcardRepositoryImpl] overrides it.
     */
    suspend fun recordReview(state: ReviewState, rating: ReviewRating, intervalDaysBefore: Int) =
        saveReviewState(state)
}