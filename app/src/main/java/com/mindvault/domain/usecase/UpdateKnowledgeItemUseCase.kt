package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.UpdateKnowledgeResult
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/**
 * Validates and saves edits. The repository keeps createdAt and sets updatedAt. When the item leaves a category,
 * [removeFromOldCategoryDeck] also removes its card from that category's deck, in the same transaction.
 */
class UpdateKnowledgeItemUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
) {
    suspend operator fun invoke(
        item: KnowledgeItem,
        removeFromOldCategoryDeck: Boolean = false,
    ): UpdateKnowledgeResult {
        val errors = validateKnowledgeFields(
            item.title, item.definition, item.personalNotes, item.example,
        )
        if (errors.isNotEmpty()) return UpdateKnowledgeResult.Invalid(errors)
        val updated = repository.update(item, removeFromOldCategoryDeck) ?: return UpdateKnowledgeResult.NotFound
        return UpdateKnowledgeResult.Updated(updated)
    }
}