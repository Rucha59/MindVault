package com.mindvault.domain.usecase

import com.mindvault.domain.error.DefaultDeckProtectedException
import com.mindvault.domain.model.DeleteDeckResult
import com.mindvault.domain.repository.DeckRepository
import javax.inject.Inject

/** Deletes a deck and its cards. Vault items are never touched. The default deck is protected. */
class DeleteDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(deckId: String): DeleteDeckResult =
        try {
            deckRepository.delete(deckId)
            DeleteDeckResult.Deleted
        } catch (e: DefaultDeckProtectedException) {
            DeleteDeckResult.DefaultProtected
        }
}