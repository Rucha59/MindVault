package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.SaveKnowledgeResult
import com.mindvault.domain.model.SaveSearchResult
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.toSaveContent
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/**
 * Saves a Search result as a normal knowledge item (WORD or CONCEPT), unless an item with the same
 * title and the same type is already saved. A word and a concept with one title can both be saved.
 */
class SaveSearchResultUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val saveKnowledgeItem: SaveKnowledgeItemUseCase,
) {
    suspend operator fun invoke(result: SearchResult): SaveSearchResult {
        val content = result.toSaveContent()
        knowledgeRepository.findByTitleAndType(content.title, content.type)
            ?.let { return SaveSearchResult.AlreadySaved(it.id) }

        val draft = KnowledgeItemDraft(
            title = content.title,
            type = content.type,
            definition = content.definition,
            personalNotes = "",
            example = content.example,
            categoryId = null,
        )
        return when (saveKnowledgeItem(draft)) {
            is SaveKnowledgeResult.Success -> SaveSearchResult.Saved
            is SaveKnowledgeResult.Invalid -> SaveSearchResult.Invalid
        }
    }
}