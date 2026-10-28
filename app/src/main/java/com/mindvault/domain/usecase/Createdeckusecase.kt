package com.mindvault.domain.usecase

import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.SaveDeckResult
import com.mindvault.domain.repository.DeckRepository
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(name: String): SaveDeckResult {
        DeckNameValidator.validate(name)?.let { return SaveDeckResult.Invalid(it) }
        return try {
            SaveDeckResult.Saved(deckRepository.create(name))
        } catch (e: DuplicateNameException) {
            SaveDeckResult.DuplicateName
        }
    }
}