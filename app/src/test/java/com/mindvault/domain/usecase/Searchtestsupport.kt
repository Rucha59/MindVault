package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.repository.SearchRepository

/** Records every query and returns whatever [outcome] is set to. */
class FakeSearchRepository(var outcome: SearchOutcome = SearchOutcome.NoResults) : SearchRepository {
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): SearchOutcome {
        queries += query
        return outcome
    }
}

fun sampleSearchResult(title: String = "hello") = SearchResult(
    title = title,
    pronunciation = null,
    meanings = listOf(
        SearchMeaning("noun", listOf(SearchDefinition("used as a greeting", "hello there"))),
    ),
    sourceName = "Test",
)

fun savedItem(id: String, title: String) = KnowledgeItem(
    id = id, title = title, type = KnowledgeType.WORD, definition = "saved definition",
    personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
)

/**
 * The one place the tests construct the save use case. If its constructor differs from
 * `SaveKnowledgeItemUseCase(knowledgeRepository)`, fix it here and every Search test follows.
 */
fun newSaveSearchResultUseCase(repository: FakeKnowledgeRepository) =
    SaveSearchResultUseCase(repository, SaveKnowledgeItemUseCase(repository))