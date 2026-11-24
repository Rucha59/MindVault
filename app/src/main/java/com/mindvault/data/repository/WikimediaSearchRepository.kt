package com.mindvault.data.repository

import android.util.Log
import com.mindvault.data.remote.ConceptDataSource
import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.data.remote.SuggestionDataSource
import com.mindvault.data.remote.wikimedia.SearchCache
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.model.savedItemKey
import com.mindvault.domain.repository.SearchRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

private const val TAG = "WikimediaSearch"

/**
 * Search V2: words from Wiktionary, concepts from Wikipedia. Maps every failure to a [SearchOutcome]
 * (offline, busy, failure) so the caller can show results from the working source. Successful answers are
 * kept for a few minutes so repeating a search does not call Wikimedia again; failures are never cached.
 * Suggestions (autocomplete) are best effort: a failure just means no suggestions.
 */
@Singleton
class WikimediaSearchRepository @Inject constructor(
    @Named("wiktionary") private val words: DefinitionDataSource,
    private val concepts: ConceptDataSource,
    @Named("wiktionary") private val wordSuggestions: SuggestionDataSource,
    @Named("wikipedia") private val conceptSuggestions: SuggestionDataSource,
) : SearchRepository {

    private val searchCache = SearchCache<List<SearchResult>>()
    private val summaryCache = SearchCache<SearchResult>()
    private val suggestionCache = SearchCache<List<String>>()

    override suspend fun search(query: String, source: SearchSource): SearchOutcome {
        val cacheKey = "${source.name}|${query.trim().lowercase()}"
        searchCache.get(cacheKey)?.let { return it.toOutcome() }
        return try {
            val results = when (source) {
                SearchSource.WORDS -> words.lookup(query)
                SearchSource.CONCEPTS -> concepts.search(query)
            }
            searchCache.put(cacheKey, results)
            results.toOutcome()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapError(e, "search $source")
        }
    }

    override suspend fun loadSummary(result: SearchResult): SearchOutcome {
        if (!result.needsSummary) return SearchOutcome.Success(listOf(result))
        val cacheKey = result.savedItemKey()
        summaryCache.get(cacheKey)?.let { return SearchOutcome.Success(listOf(it)) }
        return try {
            val loaded = concepts.loadSummary(result)
            if (loaded == null) {
                SearchOutcome.NoResults
            } else {
                summaryCache.put(cacheKey, loaded)
                SearchOutcome.Success(listOf(loaded))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapError(e, "summary")
        }
    }

    override suspend fun suggest(query: String, source: SearchSource): List<String> {
        val cacheKey = "${source.name}|${query.trim().lowercase()}"
        suggestionCache.get(cacheKey)?.let { return it }
        return try {
            val provider = when (source) {
                SearchSource.WORDS -> wordSuggestions
                SearchSource.CONCEPTS -> conceptSuggestions
            }
            provider.suggest(query).also { suggestionCache.put(cacheKey, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No suggestions from $source", e)
            emptyList()
        }
    }

    private fun List<SearchResult>.toOutcome(): SearchOutcome =
        if (isEmpty()) SearchOutcome.NoResults else SearchOutcome.Success(this)

    private fun mapError(e: Exception, what: String): SearchOutcome =
        when {
            e is IOException -> {
                Log.w(TAG, "Unreachable ($what)", e)
                SearchOutcome.Offline
            }
            e is HttpException && (e.code() == 429 || e.code() == 503) -> {
                Log.w(TAG, "Rate limited or busy ($what): HTTP ${e.code()}")
                SearchOutcome.Busy
            }
            else -> {
                Log.e(TAG, "Failed ($what)", e)
                SearchOutcome.Failure
            }
        }
}