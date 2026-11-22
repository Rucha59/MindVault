package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** The cards of one review session, read once when the session starts. */
data class ReviewQueue(
    val deckName: String,
    val cards: List<FlashcardDetail>,
    /** Cards that are extra practice only (never rated or rescheduled). */
    val extraCardIds: Set<String> = emptySet(),
)

class LoadReviewQueueUseCase @Inject constructor(
    private val observeDeckDetail: ObserveDeckDetailUseCase,
    private val observeNewCardsPerDay: ObserveNewCardsPerDayUseCase,
) {
    /** Null when the deck does not exist (any more). */
    suspend operator fun invoke(deckId: String, mode: ReviewMode): ReviewQueue? {
        val detail = observeDeckDetail(deckId).first() ?: return null
        val limit = observeNewCardsPerDay().first()
        val now = nowMillis()
        return ReviewQueue(
            deckName = detail.deck.name,
            cards = ReviewQueueBuilder.queue(mode, detail.cards, limit, now),
            extraCardIds = ReviewQueueBuilder.extraCardIds(mode, detail.cards, now),
        )
    }
}