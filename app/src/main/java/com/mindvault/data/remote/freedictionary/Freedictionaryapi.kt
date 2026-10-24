package com.mindvault.data.remote.freedictionary

import retrofit2.http.GET
import retrofit2.http.Path

/** Free Dictionary API (https://dictionaryapi.dev). No API key. Unknown words return HTTP 404. */
interface FreeDictionaryApi {
    @GET("entries/en/{word}")
    suspend fun lookup(@Path("word") word: String): List<FreeDictionaryEntryDto>
}