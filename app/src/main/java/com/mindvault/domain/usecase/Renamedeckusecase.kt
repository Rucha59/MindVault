package com.mindvault.domain.usecase

import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.SaveDeckResult
import com.mindvault.domain.repository.DeckRepository
import javax.inject.Inject

class RenameDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(deckId: String, name: String): SaveDeckResult {
        DeckNameValidator.validate(name)?.let { return SaveDeckResult.Invalid(it) }
        return try {
            deckRepository.rename(deckId, name)?.let { SaveDeckResult.Saved(it) } ?: SaveDeckResult.NotFound
        } catch (e: DuplicateNameException) {
            SaveDeckResult.DuplicateName
        }
    }
}