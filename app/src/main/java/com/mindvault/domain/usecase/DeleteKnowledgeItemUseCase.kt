package com.mindvault.domain.usecase

import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/** Deletes the item; its flashcards and review state are removed by database cascade. */
class DeleteKnowledgeItemUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
) {
    suspend operator fun invoke(itemId: String) = repository.delete(itemId)
}