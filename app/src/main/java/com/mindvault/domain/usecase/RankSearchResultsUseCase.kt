package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.savedItemKey
import javax.inject.Inject

/**
 * Orders results from all sources: exact title matches first, then titles that start with the query, then titles
 * that contain it, then the rest (similar terms). Inside one tier words come before concepts, and each source's
 * own relevance order is kept. A word and a concept may both appear; duplicates of the same kind and title are
 * removed. Pure logic, no I/O.
 */
class RankSearchResultsUseCase @Inject constructor() {

    operator fun invoke(rawQuery: String, results: List<SearchResult>): List<SearchResult> {
        val query = normalize(rawQuery)
        return results
            .distinctBy { it.savedItemKey() }
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<SearchResult>>(
                    { tier(query, it.value.title) },
                    { it.value.kind.ordinal },
                    { it.index },
                ),
            )
            .map { it.value }
    }

    private fun tier(query: String, title: String): Int {
        if (query.isEmpty()) return TIER_SIMILAR
        val normalized = normalize(title)
        return when {
            normalized == query -> TIER_EXACT
            normalized.startsWith(query) -> TIER_PREFIX
            normalized.contains(query) -> TIER_CONTAINS
            else -> TIER_SIMILAR
        }
    }

    private fun normalize(text: String): String = text.trim().lowercase().replace(WHITESPACE, " ")

    private companion object {
        const val TIER_EXACT = 0
        const val TIER_PREFIX = 1
        const val TIER_CONTAINS = 2
        const val TIER_SIMILAR = 3
        val WHITESPACE = Regex("\\s+")
    }
}