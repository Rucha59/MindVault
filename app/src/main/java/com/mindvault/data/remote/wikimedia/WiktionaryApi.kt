package com.mindvault.data.remote.wikimedia

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** English Wiktionary (https://en.wiktionary.org). No API key. An unknown term answers HTTP 404. */
interface WiktionaryApi {
    @GET("api/rest_v1/page/definition/{term}")
    suspend fun definition(@Path("term") term: String): Map<String, List<WiktionarySectionDto>>

    /** Autocomplete: `[query, [titles], [descriptions], [urls]]`, read with [OpenSearchParser]. */
    @GET("w/api.php?action=opensearch&format=json")
    suspend fun openSearch(
        @Query("search") query: String,
        @Query("limit") limit: Int,
    ): JsonElement
}