package com.mindvault.data.repository

import android.util.Log
import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.repository.SearchRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

private const val TAG = "SearchRepository"

class SearchRepositoryImpl @Inject constructor(
    private val dataSource: DefinitionDataSource,
) : SearchRepository {

    override suspend fun search(query: String): SearchOutcome =
        try {
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