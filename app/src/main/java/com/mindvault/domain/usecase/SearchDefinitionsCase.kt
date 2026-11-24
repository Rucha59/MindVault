package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.model.limited
import com.mindvault.domain.repository.SearchRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Searches the requested sources at the same time and returns one outcome per source, so a source that fails
 * does not hide the results of the other. Ranking is a separate step ([RankSearchResultsUseCase]).
 */
class SearchDefinitionsUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(rawQuery: String, sources: Set<SearchSource>): Map<SearchSource, SearchOutcome> {
        val query = rawQuery.trim().replace(WHITESPACE, " ")
        if (query.isEmpty() || query.length > SearchLimits.MAX_QUERY_LENGTH) {
            return sources.associateWith { SearchOutcome.NoResults }
        }
        return coroutineScope {
            sources
                .map { source -> async { source to searchSource(query, source) } }
                .awaitAll()
                .toMap()
        }
    }

    private suspend fun searchSource(query: String, source: SearchSource): SearchOutcome =
        when (val outcome = repository.search(query, source)) {
            is SearchOutcome.Success -> outcome.copy(results = outcome.results.map { it.limited() })
            else -> outcome
        }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}