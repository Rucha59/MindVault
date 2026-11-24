package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchResultKind
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.repository.SearchRepository

/**
 * Records every call. [search] returns the per-source entry of [outcomes] if there is one, otherwise [outcome].
 * [loadSummary] returns [summaryOutcome], or by default the result with a summary filled in.
 */
class FakeSearchRepository(var outcome: SearchOutcome = SearchOutcome.NoResults) : SearchRepository {
    val outcomes = mutableMapOf<SearchSource, SearchOutcome>()
    val calls = mutableListOf<Pair<String, SearchSource>>()
    val summaryCalls = mutableListOf<SearchResult>()
    var summaryOutcome: SearchOutcome? = null

    /** Autocomplete answers per source, and every suggest call made. */
    val suggestions = mutableMapOf<SearchSource, List<String>>()
    val suggestCalls = mutableListOf<Pair<String, SearchSource>>()

    val queries: List<String> get() = calls.map { it.first }
    val sources: List<SearchSource> get() = calls.map { it.second }

    override suspend fun search(query: String, source: SearchSource): SearchOutcome {
        calls += query to source
        return outcomes[source] ?: outcome
    }

    override suspend fun suggest(query: String, source: SearchSource): List<String> {
        suggestCalls += query to source
        return suggestions[source].orEmpty()
    }

    override suspend fun loadSummary(result: SearchResult): SearchOutcome {
        summaryCalls += result
        return summaryOutcome ?: SearchOutcome.Success(listOf(result.copy(summary = "Loaded summary of ${result.title}.")))
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

fun sampleConceptResult(title: String = "Entropy", summary: String? = "A thermodynamic state variable.") = SearchResult(
    title = title,
    pronunciation = null,
    meanings = emptyList(),
    sourceName = "Wikipedia",
    kind = SearchResultKind.CONCEPT,
    description = "Property of a thermodynamic system",
    summary = summary,
    sourceUrl = "https://en.wikipedia.org/wiki/$title",
    license = "CC BY-SA 4.0",
)

fun savedItem(id: String, title: String, type: KnowledgeType = KnowledgeType.WORD) = KnowledgeItem(
    id = id, title = title, type = type, definition = "saved definition",
    personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
)

/**
 * The one place the tests construct the save use case. If its constructor differs from
 * `SaveKnowledgeItemUseCase(knowledgeRepository)`, fix it here and every Search test follows.
 */
fun newSaveSearchResultUseCase(repository: FakeKnowledgeRepository) =
    SaveSearchResultUseCase(repository, SaveKnowledgeItemUseCase(repository))