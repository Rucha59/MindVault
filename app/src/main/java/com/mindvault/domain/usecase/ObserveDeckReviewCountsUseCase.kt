package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ReviewCounts
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Live due / new / difficult counts for one deck. Re-emits when its cards or the daily limit change. */
class ObserveDeckReviewCountsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val observeNewCardsPerDay: ObserveNewCardsPerDayUseCase,
) {
    operator fun invoke(deckId: String): Flow<ReviewCounts> = combine(
        flashcardRepository.observeByDeck(deckId),
        observeNewCardsPerDay(),
    ) { cards, limit -> ReviewQueueBuilder.counts(cards, limit, nowMillis()) }
}