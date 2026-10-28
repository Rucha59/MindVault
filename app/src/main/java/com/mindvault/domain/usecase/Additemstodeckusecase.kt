package com.mindvault.domain.usecase

import com.mindvault.domain.model.AddItemsToDeckResult
import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Makes a card for each Vault item in the deck. Items already in the deck are skipped, never duplicated. */
class AddItemsToDeckUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(deckId: String, knowledgeItemIds: Collection<String>): AddItemsToDeckResult {
        var added = 0
        var alreadyInDeck = 0
        knowledgeItemIds.distinct().forEach { itemId ->
            when (flashcardRepository.addToDeck(itemId, deckId)) {
                is AddToDeckResult.Added -> added++
                AddToDeckResult.AlreadyInDeck -> alreadyInDeck++
            }
        }
        return AddItemsToDeckResult(added, alreadyInDeck)
    }
}