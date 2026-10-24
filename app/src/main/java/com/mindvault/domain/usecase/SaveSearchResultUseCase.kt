package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SaveKnowledgeResult
import com.mindvault.domain.model.SaveSearchResult
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.toSaveContent
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject

/** Saves a Search result as a normal knowledge item, unless a word with that title is already saved. */
class SaveSearchResultUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val saveKnowledgeItem: SaveKnowledgeItemUseCase,
) {
    suspend operator fun invoke(result: SearchResult): SaveSearchResult {
        val content = result.toSaveContent()
        knowledgeRepository.findByTitle(content.title)?.let { return SaveSearchResult.AlreadySaved(it.id) }

        val draft = KnowledgeItemDraft(
            title = content.title,
            type = KnowledgeType.WORD,
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