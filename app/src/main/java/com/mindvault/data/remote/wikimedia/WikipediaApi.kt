package com.mindvault.data.remote.wikimedia

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** English Wikipedia (https://en.wikipedia.org). No API key. */
interface WikipediaApi {
    @GET("w/rest.php/v1/search/page")
    suspend fun searchPages(
        @Query("q") query: String,
        @Query("limit") limit: Int,
    ): WikipediaSearchResponseDto

    /** [title] is the page key (underscores for spaces). Redirects are followed by OkHttp. */
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun summary(@Path("title") title: String): WikipediaSummaryDto

    /** Autocomplete: `[query, [titles], [descriptions], [urls]]`, read with [OpenSearchParser]. */
    @GET("w/api.php?action=opensearch&format=json")
    suspend fun openSearch(
        @Query("search") query: String,
        @Query("limit") limit: Int,
    ): JsonElement
}