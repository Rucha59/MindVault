package com.mindvault.domain.usecase

import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.DeckSummary
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Every deck (default first) with live card counts. Re-emits on any deck or card change. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDeckSummariesUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) {
    operator fun invoke(): Flow<List<DeckSummary>> =
        deckRepository.observeAll().flatMapLatest { decks ->
            if (decks.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(decks.map { deck -> summaryOf(deck) }) { summaries -> summaries.toList() }
            }
        }

    private fun summaryOf(deck: Deck): Flow<DeckSummary> = combine(
        flashcardRepository.observeCountByDeck(deck.id),
        flashcardRepository.observeUnreviewedCountByDeck(deck.id),
    ) { total, unreviewed -> DeckSummary(deck, total, unreviewed) }
}