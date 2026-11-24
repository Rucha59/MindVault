package com.mindvault.domain.repository

import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSource

interface SearchRepository {
    /** Looks the query up in one external source. Does not save anything. */
    suspend fun search(query: String, source: SearchSource): SearchOutcome

    /**
     * Loads the full summary of a concept that was returned without one. Success carries one result with its
     * summary; NoResults means the source has no usable summary for it (so it cannot be saved).
     */
    suspend fun loadSummary(result: SearchResult): SearchOutcome

    /**
     * Complete titles that start like [query], for autocomplete. Best effort: an unreachable or failing source
     * gives an empty list instead of an error (a missing suggestion is not worth a message).
     */
    suspend fun suggest(query: String, source: SearchSource): List<String>
}