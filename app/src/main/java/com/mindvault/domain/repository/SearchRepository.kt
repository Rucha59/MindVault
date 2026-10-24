package com.mindvault.domain.repository

import com.mindvault.domain.model.SearchOutcome

interface SearchRepository {
    /** Looks the query up in the external source. Does not save anything. */
    suspend fun search(query: String): SearchOutcome
}