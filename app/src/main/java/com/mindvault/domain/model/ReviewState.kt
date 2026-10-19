package com.mindvault.domain.model

/** Storage only. Scheduling logic (computing the next state) arrives in a later phase. */
data class ReviewState(
    val flashcardId: String,
    val lastReviewedAt: Long?,
    val nextReviewAt: Long,
    val reviewCount: Int,
    val easeFactor: Double,
    val intervalDays: Int,
) {
    val isReviewed: Boolean get() = reviewCount > 0

    companion object {
        const val DEFAULT_EASE_FACTOR = 2.5

        /** A new card is due immediately and has never been reviewed. */
        fun initial(flashcardId: String, now: Long) = ReviewState(
            flashcardId = flashcardId,
            lastReviewedAt = null,
            nextReviewAt = now,
            reviewCount = 0,
            easeFactor = DEFAULT_EASE_FACTOR,
            intervalDays = 0,
        )
    }
}