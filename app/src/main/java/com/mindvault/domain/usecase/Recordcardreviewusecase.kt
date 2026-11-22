package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.scheduler.ReviewScheduler
import javax.inject.Inject

/** Saves rated reviews: the scheduler computes the next state, the repository stores it (and queues sync). */
class RecordCardReviewUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val scheduler: ReviewScheduler,
) {
    /** A card's first rating in a session. */
    suspend operator fun invoke(flashcardId: String, rating: ReviewRating) {
        val now = nowMillis()
        val state = flashcardRepository.getReviewState(flashcardId) ?: ReviewState.initial(flashcardId, now)
        flashcardRepository.saveReviewState(scheduler.next(state, rating, now))
    }

    /** The answer when a card marked Again comes back in the same session. Does nothing if the card is gone. */
    suspend fun relearn(flashcardId: String, rating: ReviewRating) {
        val state = flashcardRepository.getReviewState(flashcardId) ?: return
        flashcardRepository.saveReviewState(scheduler.relearn(state, rating, nowMillis()))
    }
}