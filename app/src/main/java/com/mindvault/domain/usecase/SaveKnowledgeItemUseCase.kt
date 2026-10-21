package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.SaveKnowledgeResult
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/** Validates a draft and saves it as a new KnowledgeItem. Shared by manual Add and, later, Search. */
class SaveKnowledgeItemUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
) {
    suspend operator fun invoke(draft: KnowledgeItemDraft): SaveKnowledgeResult {
        val errors = validateKnowledgeFields(
            draft.title, draft.definition, draft.personalNotes, draft.example,
        )
        if (errors.isNotEmpty()) return SaveKnowledgeResult.Invalid(errors)
        return SaveKnowledgeResult.Success(repository.create(draft))
    }

    companion object {
        const val MAX_TITLE_LENGTH = 200
        const val MAX_DEFINITION_LENGTH = 5000
        const val MAX_NOTES_LENGTH = 5000
        const val MAX_EXAMPLE_LENGTH = 1000
    }
}