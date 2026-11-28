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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only Hard, Good and Easy are logged. Again is not; the answer after it is. */
class RecordCardReviewLogTest {

    private class RecordingRepository(var state: ReviewState?) : FlashcardRepository {
        data class Recorded(val state: ReviewState, val rating: ReviewRating, val intervalDaysBefore: Int)

        val recorded = mutableListOf<Recorded>()
        var savedDirectly: ReviewState? = null

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
            savedDirectly = state
        }
        override suspend fun recordReview(state: ReviewState, rating: ReviewRating, intervalDaysBefore: Int) {
            recorded += Recorded(state, rating, intervalDaysBefore)
        }
    }

    private val scheduler = Sm2Scheduler()

    private fun reviewed() = ReviewState(
        flashcardId = "c", lastReviewedAt = 1L, nextReviewAt = 2L, reviewCount = 2, easeFactor = 2.5, intervalDays = 3,
    )

    /** What the card looks like right after its first rating was Again. */
    private fun afterAgain() = ReviewState(
        flashcardId = "c", lastReviewedAt = 1L, nextReviewAt = 2L, reviewCount = 1, easeFactor = 2.3, intervalDays = 1,
    )

    @Test
    fun aGoodFirstRatingIsLoggedWithThePreviousInterval() = runBlocking {
        val repo = RecordingRepository(reviewed())
        RecordCardReviewUseCase(repo, scheduler)("c", ReviewRating.GOOD)

        val entry = repo.recorded.single()
        assertEquals(ReviewRating.GOOD, entry.rating)
        assertEquals(3, entry.intervalDaysBefore)
        assertEquals(3, entry.state.reviewCount)
        assertNull("the log path replaces the plain save", repo.savedDirectly)
    }

    @Test
    fun aNewCardRatedHardIsLoggedWithAnIntervalOfZero() = runBlocking {
        val repo = RecordingRepository(null)
        RecordCardReviewUseCase(repo, scheduler)("c", ReviewRating.HARD)

        assertEquals(ReviewRating.HARD, repo.recorded.single().rating)
        assertEquals(0, repo.recorded.single().intervalDaysBefore)
    }

    @Test
    fun againOnTheFirstRatingIsSavedButNotLogged() = runBlocking {
        val repo = RecordingRepository(reviewed())
        RecordCardReviewUseCase(repo, scheduler)("c", ReviewRating.AGAIN)

        assertTrue(repo.recorded.isEmpty())
        assertNotNull("the schedule still changes", repo.savedDirectly)
        assertEquals(3, repo.savedDirectly!!.reviewCount)
    }

    @Test
    fun theAnswerAfterAgainIsLoggedAndItIsTheOnlyEntry() = runBlocking {
        val repo = RecordingRepository(afterAgain())
        RecordCardReviewUseCase(repo, scheduler).relearn("c", ReviewRating.EASY)

        val entry = repo.recorded.single()
        assertEquals(ReviewRating.EASY, entry.rating)
        assertEquals(1, entry.intervalDaysBefore)
        assertEquals(3, entry.state.intervalDays)
        assertEquals(1, entry.state.reviewCount) // relearn still does not change the review count
        assertNull(repo.savedDirectly)
    }

    @Test
    fun relearnWithAgainIsNeverLogged() = runBlocking {
        val repo = RecordingRepository(afterAgain())
        RecordCardReviewUseCase(repo, scheduler).relearn("c", ReviewRating.AGAIN)

        assertTrue(repo.recorded.isEmpty())
        assertNotNull(repo.savedDirectly)
    }

    @Test
    fun relearnOfAMissingCardDoesNothing() = runBlocking {
        val repo = RecordingRepository(null)
        RecordCardReviewUseCase(repo, scheduler).relearn("c", ReviewRating.GOOD)

        assertTrue(repo.recorded.isEmpty())
        assertNull(repo.savedDirectly)
    }
}