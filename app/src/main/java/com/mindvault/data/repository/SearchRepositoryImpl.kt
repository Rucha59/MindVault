package com.mindvault.data.repository

import android.util.Log
import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.repository.SearchRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

private const val TAG = "SearchRepository"

/**
 * Search V1: words only, from the Free Dictionary API (see SearchProviderModule). Kept so the app can switch
 * back by binding this class in RepositoryModule. V1 has no concepts, so that source never has a result.
 */
class SearchRepositoryImpl @Inject constructor(
    private val dataSource: DefinitionDataSource,
) : SearchRepository {

    override suspend fun search(query: String, source: SearchSource): SearchOutcome {
        if (source != SearchSource.WORDS) return SearchOutcome.NoResults
        return try {
            val results = dataSource.lookup(query)
            if (results.isEmpty()) SearchOutcome.NoResults else SearchOutcome.Success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Search provider unreachable", e)
            SearchOutcome.Offline
        } catch (e: Exception) {
            Log.e(TAG, "Search provider failed", e)
            SearchOutcome.Failure
        }
    }

    /** V1 never returns a concept without its summary, so there is nothing to load. */
    override suspend fun loadSummary(result: SearchResult): SearchOutcome = SearchOutcome.Success(listOf(result))

    /** V1 has no autocomplete source. */
    override suspend fun suggest(query: String, source: SearchSource): List<String> = emptyList()
}