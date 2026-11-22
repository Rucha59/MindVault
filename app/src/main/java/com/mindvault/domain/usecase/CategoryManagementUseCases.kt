package com.mindvault.domain.usecase

import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.RenameCategoryResult
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.repository.CategoryRepository
import com.mindvault.domain.repository.DeckRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The flashcard deck of each category that has one, by category ID (the oldest, if a category has several). */
class ObserveCategoryDecksUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    operator fun invoke(): Flow<Map<String, Deck>> = deckRepository.observeAll().map { decks ->
        decks.filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.minWith(compareBy<Deck>({ it.createdAt }, { it.id })) }
    }
}

class RenameCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: String, name: String): RenameCategoryResult {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> return RenameCategoryResult.Invalid(ValidationError.BLANK)
            trimmed.length > CreateCategoryUseCase.MAX_NAME_LENGTH ->
                return RenameCategoryResult.Invalid(ValidationError.TOO_LONG)
        }
        return try {
            repository.rename(categoryId, trimmed)?.let { RenameCategoryResult.Renamed(it) }
                ?: RenameCategoryResult.NotFound
        } catch (e: DuplicateNameException) {
            RenameCategoryResult.DuplicateName
        }
    }
}

/**
 * Deletes a category. Its items stay in the Vault as Uncategorized. Its deck is kept as an ordinary deck, or deleted
 * with its cards when [deleteDeck] is true. The deck is looked up first (deleting the category clears the link);
 * if deleting the deck then fails, it simply stays as an ordinary deck.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(categoryId: String, deleteDeck: Boolean) {
        val deck = if (deleteDeck) deckRepository.findCategoryDeck(categoryId) else null
        categoryRepository.delete(categoryId)
        deck?.let { deckRepository.delete(it.id) }
    }
}

/** Creates the deck of a category that has none, with a card for each of its items. Returns the deck. */
class CreateCategoryDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(categoryId: String): Deck? = deckRepository.ensureCategoryDeck(categoryId)
}