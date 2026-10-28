package com.mindvault.domain.usecase

import com.mindvault.domain.model.DeckOption
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Decks (default first) flagged with whether the given Vault item already has a card there. */
class ObserveDeckOptionsUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) {
    operator fun invoke(knowledgeItemId: String): Flow<List<DeckOption>> = combine(
        deckRepository.observeAll(),
        flashcardRepository.observeDeckIdsContaining(knowledgeItemId),
    ) { decks, deckIdsWithItem ->
        val withItem = deckIdsWithItem.toSet()
        decks.map { deck -> DeckOption(deck, deck.id in withItem) }
    }
}