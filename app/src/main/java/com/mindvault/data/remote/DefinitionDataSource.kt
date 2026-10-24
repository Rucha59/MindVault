package com.mindvault.data.remote

import com.mindvault.domain.model.SearchResult

/**
 * The seam for swapping the external Search provider. See docs/SEARCH_PROVIDER.md.
 *
 * Contract for every implementation:
 * - Return an empty list when the provider has no match. Never invent a result.
 * - Throw [java.io.IOException] when the provider cannot be reached (offline, timeout).
 * - Throw any other exception for a bad response; the repository reports it as a failure.
 * - Return one [SearchResult] per word, already mapped to domain models and trimmed.
 */
interface DefinitionDataSource {
    suspend fun lookup(query: String): List<SearchResult>
}