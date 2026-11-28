package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.scheduler.ReviewScheduler
import javax.inject.Inject

/**
 * Saves rated reviews: the scheduler computes the next state, the repository stores it (and queues sync).
 *
 * Review history: only Hard, Good and Easy are logged. Again is not a category: a card rated Again is saved but not
 * logged, and its answer when it comes back in the same session (Hard, Good or Easy) is the one that is logged.
 * Extra practice never reaches this class, so it is never logged.
 */
class RecordCardReviewUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val scheduler: ReviewScheduler,
) {
    /** A card's first rating in a session. Logged unless it is Again (then its later answer is logged). */
    suspend operator fun invoke(flashcardId: String, rating: ReviewRating) {
        val now = nowMillis()
        val state = flashcardRepository.getReviewState(flashcardId) ?: ReviewState.initial(flashcardId, now)
        save(scheduler.next(state, rating, now), rating, state.intervalDays)
    }

    /**
     * The answer when a card marked Again comes back in the same session. This is the answer that is logged.
     * Does nothing if the card is gone.
     */
    suspend fun relearn(flashcardId: String, rating: ReviewRating) {
        val state = flashcardRepository.getReviewState(flashcardId) ?: return
        save(scheduler.relearn(state, rating, nowMillis()), rating, state.intervalDays)
    }

    private suspend fun save(next: ReviewState, rating: ReviewRating, intervalDaysBefore: Int) {
        if (rating == ReviewRating.AGAIN) {
            flashcardRepository.saveReviewState(next)
        } else {
            flashcardRepository.recordReview(next, rating, intervalDaysBefore)
        }
    }
}