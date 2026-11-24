package com.mindvault.data.remote.wikimedia

import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.data.remote.SuggestionDataSource
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchResult
import javax.inject.Inject
import retrofit2.HttpException

/**
 * Word definitions from English Wiktionary. Terms are case-sensitive there, so a miss on a capitalised query
 * is retried once in lower case. HTTP 404 means "no result", not an error. Also the source of word suggestions
 * (Wiktionary's title search also lists words of other languages; they are only suggestions).
 */
class WiktionaryDataSource @Inject constructor(
    private val api: WiktionaryApi,
) : DefinitionDataSource, SuggestionDataSource {

    override suspend fun lookup(query: String): List<SearchResult> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        for (candidate in listOf(term, term.lowercase()).distinct()) {
            fetch(candidate)?.let { return listOf(it) }
        }
        return emptyList()
    }

    override suspend fun suggest(query: String): List<String> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return OpenSearchParser.titles(api.openSearch(term, SUGGESTION_LIMIT))
    }

    private suspend fun fetch(term: String): SearchResult? =
        try {
            WiktionaryMapper.toWordResult(term, api.definition(term))
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }

    private companion object {
        const val SUGGESTION_LIMIT = SearchLimits.MAX_SUGGESTIONS
    }
}