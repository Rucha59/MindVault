package com.mindvault.domain.usecase

import com.mindvault.domain.model.DeckDetail
import com.mindvault.domain.model.DeckSummary
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** A deck with its cards (newest first). Emits null when the deck does not exist (any more). */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDeckDetailUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) {
    operator fun invoke(deckId: String): Flow<DeckDetail?> =
        deckRepository.observeById(deckId).flatMapLatest { deck ->
            if (deck == null) {
                flowOf(null)
            } else {
                flashcardRepository.observeByDeck(deckId).map { cards ->
                    val unreviewed = cards.count { !it.reviewState.isReviewed }
                    DeckDetail(DeckSummary(deck, cards.size, unreviewed), cards)
                }
            }
        }
}