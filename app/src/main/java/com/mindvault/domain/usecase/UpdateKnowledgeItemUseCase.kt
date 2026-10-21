package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.UpdateKnowledgeResult
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/** Validates and saves edits. The repository keeps createdAt and sets updatedAt. */
class UpdateKnowledgeItemUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
) {
    suspend operator fun invoke(item: KnowledgeItem): UpdateKnowledgeResult {
        val errors = validateKnowledgeFields(
            item.title, item.definition, item.personalNotes, item.example,
        )
        if (errors.isNotEmpty()) return UpdateKnowledgeResult.Invalid(errors)
        val updated = repository.update(item) ?: return UpdateKnowledgeResult.NotFound
        return UpdateKnowledgeResult.Updated(updated)
    }
}