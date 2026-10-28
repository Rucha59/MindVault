package com.mindvault.domain.usecase

import com.mindvault.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Removes one card (and its review progress) from its deck. The Vault item stays. */
class RemoveCardFromDeckUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(flashcardId: String) = flashcardRepository.removeFromDeck(flashcardId)
}