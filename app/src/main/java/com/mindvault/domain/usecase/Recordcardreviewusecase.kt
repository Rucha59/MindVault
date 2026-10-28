package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject

/**
 * Records one review: count + 1 and the review time. Scheduling fields (next review, ease,
 * interval) are deliberately left alone until a scheduler exists.
 */
class RecordCardReviewUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(flashcardId: String) {
        val now = nowMillis()
        val state = flashcardRepository.getReviewState(flashcardId) ?: ReviewState.initial(flashcardId, now)
        flashcardRepository.saveReviewState(
            state.copy(reviewCount = state.reviewCount + 1, lastReviewedAt = now),
        )
    }
}