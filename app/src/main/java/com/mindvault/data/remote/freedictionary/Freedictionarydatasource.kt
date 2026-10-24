package com.mindvault.data.remote.freedictionary

import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.domain.model.SearchResult
import javax.inject.Inject
import retrofit2.HttpException

class FreeDictionaryDataSource @Inject constructor(
    private val api: FreeDictionaryApi,
) : DefinitionDataSource {

    override suspend fun lookup(query: String): List<SearchResult> =
        try {
            api.lookup(query).toSearchResults()
        } catch (e: HttpException) {
            // This provider answers 404 for words it does not know. That is "no result", not an error.
            if (e.code() == 404) emptyList() else throw e
        }
}