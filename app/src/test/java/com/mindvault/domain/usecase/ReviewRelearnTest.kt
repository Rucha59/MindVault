package com.mindvault.domain.usecase

import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.scheduler.Sm2Scheduler
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewRelearnTest {

    private val scheduler = Sm2Scheduler()
    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

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

    /** What the card looks like right after its first rating was Again. */
    private fun afterAgain() = ReviewState(
        flashcardId = "c", lastReviewedAt = now, nextReviewAt = now, reviewCount = 1,
        easeFactor = 2.3, intervalDays = 1,
    )

    @Test
    fun relearnIntervalsPerRating() {
        assertEquals(1, scheduler.relearnIntervalDays(ReviewRating.HARD))
        assertEquals(2, scheduler.relearnIntervalDays(ReviewRating.GOOD))
        assertEquals(3, scheduler.relearnIntervalDays(ReviewRating.EASY))
    }

    @Test
    fun relearnChangesOnlyTheIntervalAndDueDate() {
        val next = scheduler.relearn(afterAgain(), ReviewRating.GOOD, now, zone)
        assertEquals(2, next.intervalDays)
        assertEquals(
            LocalDate.of(2026, 11, 20).atStartOfDay(zone).toInstant().toEpochMilli(),
            next.nextReviewAt,
        )
        assertEquals(2.3, next.easeFactor, 0.0) // not penalised twice
        assertEquals(1, next.reviewCount)       // still counts as the card's first review
    }

    @Test
    fun useCaseSavesTheRelearnedState() = runBlocking {
        val repo = FakeFlashcardRepository(afterAgain())
        RecordCardReviewUseCase(repo, scheduler).relearn("c", ReviewRating.EASY)
        assertEquals(3, repo.saved!!.intervalDays)
        assertEquals(1, repo.saved!!.reviewCount)
        assertEquals(2.3, repo.saved!!.easeFactor, 0.0)
    }

    @Test
    fun useCaseDoesNothingWhenTheCardIsGone() = runBlocking {
        val repo = FakeFlashcardRepository(null)
        RecordCardReviewUseCase(repo, scheduler).relearn("c", ReviewRating.GOOD)
        assertNull(repo.saved)
    }
}