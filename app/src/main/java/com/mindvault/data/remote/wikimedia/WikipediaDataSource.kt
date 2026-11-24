package com.mindvault.data.remote.wikimedia

import com.mindvault.data.remote.ConceptDataSource
import com.mindvault.data.remote.SuggestionDataSource
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

/**
 * Concepts from English Wikipedia. One search request, then the summaries of the best [EAGER_SUMMARIES] hits;
 * the other hits keep only their title and description until the user opens or saves them. This keeps a search
 * to a few requests (Wikimedia rate-limits clients). Also the source of concept suggestions.
 */
class WikipediaDataSource @Inject constructor(
    private val api: WikipediaApi,
) : ConceptDataSource, SuggestionDataSource {

    override suspend fun search(query: String): List<SearchResult> {
        val hits = WikipediaMapper.toHits(api.searchPages(query.trim(), SEARCH_LIMIT))
        if (hits.isEmpty()) return emptyList()
        return coroutineScope {
            hits
                .mapIndexed { index, hit ->
                    async { if (index < EAGER_SUMMARIES) withSummary(hit) else WikipediaMapper.toLazyResult(hit) }
                }
                .awaitAll()
                .filterNotNull()
        }
    }

    override suspend fun suggest(query: String): List<String> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return OpenSearchParser.titles(api.openSearch(term, SUGGESTION_LIMIT), dropDisambiguation = true)
    }

    override suspend fun loadSummary(result: SearchResult): SearchResult? =
        try {
            val key = result.title.trim().replace(' ', '_')
            WikipediaMapper.toConceptResult(api.summary(key), fallbackDescription = result.description)
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }

    /**
     * The hit with its summary. A page that turns out not to be a normal article (404, disambiguation, no text)
     * is dropped. If the summary request merely fails, the hit stays as a card that loads it later.
     */
    private suspend fun withSummary(hit: ConceptHit): SearchResult? =
        try {
            WikipediaMapper.toConceptResult(api.summary(hit.key), fallbackDescription = hit.description)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404) null else WikipediaMapper.toLazyResult(hit)
        } catch (e: Exception) {
            WikipediaMapper.toLazyResult(hit)
        }

    private companion object {
        const val SEARCH_LIMIT = 5
        const val EAGER_SUMMARIES = 2
        const val SUGGESTION_LIMIT = SearchLimits.MAX_SUGGESTIONS
    }
}