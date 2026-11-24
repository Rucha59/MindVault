package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.repository.SearchRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Autocomplete: complete words and concept titles for what the user has typed so far, from the sources the
 * filter allows. Sources answer at the same time; their suggestions are interleaved (so one source cannot push
 * out the other), duplicates are removed ignoring case, an exact match comes first, and at most
 * [SearchLimits.MAX_SUGGESTIONS] are returned. Returns nothing for fewer than
 * [SearchLimits.MIN_SUGGESTION_LENGTH] characters.
 */
class SuggestSearchTermsUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(rawQuery: String, sources: Set<SearchSource>): List<String> {
        val query = rawQuery.trim().replace(WHITESPACE, " ")
        if (query.length < SearchLimits.MIN_SUGGESTION_LENGTH || query.length > SearchLimits.MAX_QUERY_LENGTH) {
            return emptyList()
        }
        val perSource: List<List<String>> = coroutineScope {
            sources
                .sortedBy { it.ordinal }
                .map { source -> async { repository.suggest(query, source) } }
                .awaitAll()
        }
        val rounds = perSource.maxOfOrNull { it.size } ?: 0
        val interleaved = (0 until rounds).flatMap { index -> perSource.mapNotNull { it.getOrNull(index) } }
        val lowerQuery = query.lowercase()
        return interleaved
            .distinctBy { it.lowercase() }
            .sortedBy { if (it.lowercase() == lowerQuery) 0 else 1 } // stable: exact match first, order kept
            .take(SearchLimits.MAX_SUGGESTIONS)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}