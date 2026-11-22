package com.mindvault.domain.usecase

import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.scheduler.Sm2Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecordCardReviewUseCaseTest {

    private class FakeFlashcardRepository(var state: ReviewState?) : FlashcardRepository {
        var saved: ReviewState? = null
        override fun observeByDeck(deckId: String): Flow<List<FlashcardDetail>> = emptyFlow()
        override fun observeDue(deckId: String, now: Long): Flow<List<FlashcardDetail>> = emptyFlow()
        override fun observeCountByDeck(deckId: String): Flow<Int> = emptyFlow()
        override fun observeUnreviewedCountByDeck(deckId: String): Flow<Int> = emptyFlow()
        override fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int> = emptyFlow()
        override fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>> = emptyFlow()
        override suspend fun addToDeck(knowledgeItemId: String, deckId: String): AddToDeckResult = error("unused")
        override suspend fun removeFromDeck(flashcardId: String) = error("unused")
        override suspend fun getReviewState(flashcardId: String): ReviewState? = state
        override suspend fun saveReviewState(state: ReviewState) {
            saved = state
        }
    }

    @Test
    fun ratingASavesTheScheduledState() = runBlocking {
        val repo = FakeFlashcardRepository(ReviewState.initial("c", 0L))
        RecordCardReviewUseCase(repo, Sm2Scheduler())("c", ReviewRating.GOOD)
        val saved = repo.saved
        assertNotNull(saved)
        assertEquals(1, saved!!.reviewCount)
        assertEquals(2, saved.intervalDays)
        assertNotNull(saved.lastReviewedAt)
    }

    @Test
    fun aMissingStateStartsFromANewCard() = runBlocking {
        val repo = FakeFlashcardRepository(null)
        RecordCardReviewUseCase(repo, Sm2Scheduler())("c", ReviewRating.AGAIN)
        assertEquals(1, repo.saved!!.intervalDays)
        assertEquals(2.3, repo.saved!!.easeFactor, 0.0)
    }
}