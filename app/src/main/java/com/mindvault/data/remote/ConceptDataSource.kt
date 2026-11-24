package com.mindvault.data.remote

import com.mindvault.domain.model.SearchResult

/**
 * The seam for the concept source (Wikipedia). Same rules as [DefinitionDataSource]:
 * - [search] returns an empty list when there is no match. Never invent a result.
 * - Throw [java.io.IOException] when the source cannot be reached (offline, timeout).
 * - Throw any other exception for a bad response; the repository reports it as a failure.
 */
interface ConceptDataSource {
    /** Concept results, best match first. The first few carry a summary; the rest are loaded with [loadSummary]. */
    suspend fun search(query: String): List<SearchResult>

    /** Returns [result] with its summary, or null when the source has no usable summary for it. */
    suspend fun loadSummary(result: SearchResult): SearchResult?
}