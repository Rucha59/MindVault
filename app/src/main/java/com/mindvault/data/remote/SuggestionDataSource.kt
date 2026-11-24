package com.mindvault.data.remote

/** Autocomplete from one external source: titles that start like the query. */
interface SuggestionDataSource {
    /** Throws [java.io.IOException] when the source cannot be reached; any other exception for a bad response. */
    suspend fun suggest(query: String): List<String>
}