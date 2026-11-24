package com.mindvault.data.repository

import com.mindvault.data.remote.ConceptDataSource
import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.data.remote.SuggestionDataSource
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.usecase.sampleConceptResult
import com.mindvault.domain.usecase.sampleSearchResult
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class WikimediaSearchRepositoryTest {

    private class FakeWords(var block: suspend (String) -> List<SearchResult>) : DefinitionDataSource {
        var calls = 0
        override suspend fun lookup(query: String): List<SearchResult> {
            calls++
            return block(query)
        }
    }

    private class FakeConcepts(
        var searchBlock: suspend (String) -> List<SearchResult> = { emptyList() },
        var summaryBlock: suspend (SearchResult) -> SearchResult? = { null },
    ) : ConceptDataSource {
        var searchCalls = 0
        var summaryCalls = 0
        override suspend fun search(query: String): List<SearchResult> {
            searchCalls++
            return searchBlock(query)
        }

        override suspend fun loadSummary(result: SearchResult): SearchResult? {
            summaryCalls++
            return summaryBlock(result)
        }
    }

    private class FakeSuggestions(var block: suspend (String) -> List<String> = { emptyList() }) : SuggestionDataSource {
        var calls = 0
        override suspend fun suggest(query: String): List<String> {
            calls++
            return block(query)
        }
    }

    private val words = FakeWords { listOf(sampleSearchResult("entropy")) }
    private val concepts = FakeConcepts()
    private val wordSuggestions = FakeSuggestions { listOf("entropy", "entry") }
    private val conceptSuggestions = FakeSuggestions { listOf("Entropy") }
    private val repo = WikimediaSearchRepository(words, concepts, wordSuggestions, conceptSuggestions)

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun wordsSource_usesTheWordDataSourceOnly() = runBlocking {
        val outcome = repo.search("entropy", SearchSource.WORDS)

        assertEquals(SearchOutcome.Success(listOf(sampleSearchResult("entropy"))), outcome)
        assertEquals(1, words.calls)
        assertEquals(0, concepts.searchCalls)
    }

    @Test
    fun conceptsSource_usesTheConceptDataSourceOnly() = runBlocking {
        concepts.searchBlock = { listOf(sampleConceptResult("Entropy")) }

        val outcome = repo.search("entropy", SearchSource.CONCEPTS)

        assertEquals(SearchOutcome.Success(listOf(sampleConceptResult("Entropy"))), outcome)
        assertEquals(0, words.calls)
        assertEquals(1, concepts.searchCalls)
    }

    @Test
    fun emptyList_isNoResults() = runBlocking {
        assertEquals(SearchOutcome.NoResults, repo.search("zzqxv", SearchSource.CONCEPTS))
    }

    @Test
    fun ioException_isOffline() = runBlocking {
        words.block = { throw IOException("no network") }
        assertEquals(SearchOutcome.Offline, repo.search("entropy", SearchSource.WORDS))
    }

    @Test
    fun rateLimitedAndBusyAnswers_areBusy() = runBlocking {
        words.block = { throw http(429) }
        assertEquals(SearchOutcome.Busy, repo.search("entropy", SearchSource.WORDS))

        words.block = { throw http(503) }
        assertEquals(SearchOutcome.Busy, repo.search("another", SearchSource.WORDS))
    }

    @Test
    fun otherHttpErrorsAndBadBodies_areFailures() = runBlocking {
        words.block = { throw http(500) }
        assertEquals(SearchOutcome.Failure, repo.search("entropy", SearchSource.WORDS))

        words.block = { throw IllegalStateException("malformed") }
        assertEquals(SearchOutcome.Failure, repo.search("another", SearchSource.WORDS))
    }

    @Test
    fun repeatedSearch_isAnsweredFromTheCache_ignoringCaseAndSpaces() = runBlocking {
        repo.search("entropy", SearchSource.WORDS)
        repo.search("  Entropy ", SearchSource.WORDS)

        assertEquals(1, words.calls)
    }

    @Test
    fun theSameQueryOnTheOtherSource_isNotServedFromTheCache() = runBlocking {
        repo.search("entropy", SearchSource.WORDS)
        repo.search("entropy", SearchSource.CONCEPTS)

        assertEquals(1, words.calls)
        assertEquals(1, concepts.searchCalls)
    }

    @Test
    fun failures_areNotCached() = runBlocking {
        words.block = { throw IOException("down") }
        assertEquals(SearchOutcome.Offline, repo.search("entropy", SearchSource.WORDS))

        words.block = { listOf(sampleSearchResult("entropy")) }
        assertEquals(
            SearchOutcome.Success(listOf(sampleSearchResult("entropy"))),
            repo.search("entropy", SearchSource.WORDS),
        )
        assertEquals(2, words.calls)
    }

    @Test
    fun loadSummary_returnsTheResultWithItsSummary_andCachesIt() = runBlocking {
        val lazy = sampleConceptResult("Entropy", summary = null)
        concepts.summaryBlock = { it.copy(summary = "Loaded.") }

        val first = repo.loadSummary(lazy) as SearchOutcome.Success
        repo.loadSummary(lazy)

        assertEquals("Loaded.", first.results.single().summary)
        assertEquals(1, concepts.summaryCalls)
    }

    @Test
    fun loadSummary_withNoUsableSummary_isNoResults() = runBlocking {
        concepts.summaryBlock = { null }

        assertEquals(SearchOutcome.NoResults, repo.loadSummary(sampleConceptResult(summary = null)))
    }

    @Test
    fun loadSummary_failuresMapLikeSearchFailures() = runBlocking {
        val lazy = sampleConceptResult(summary = null)

        concepts.summaryBlock = { throw IOException("down") }
        assertEquals(SearchOutcome.Offline, repo.loadSummary(lazy))

        concepts.summaryBlock = { throw http(429) }
        assertEquals(SearchOutcome.Busy, repo.loadSummary(lazy))

        concepts.summaryBlock = { throw IllegalStateException("bad") }
        assertEquals(SearchOutcome.Failure, repo.loadSummary(lazy))
    }

    @Test
    fun suggestions_comeFromTheRightSourceOnly() = runBlocking {
        assertEquals(listOf("entropy", "entry"), repo.suggest("en", SearchSource.WORDS))
        assertEquals(listOf("Entropy"), repo.suggest("en", SearchSource.CONCEPTS))
        assertEquals(1, wordSuggestions.calls)
        assertEquals(1, conceptSuggestions.calls)
    }

    @Test
    fun repeatedSuggestions_areAnsweredFromTheCache_ignoringCaseAndSpaces() = runBlocking {
        repo.suggest("en", SearchSource.WORDS)
        repo.suggest("  EN ", SearchSource.WORDS)

        assertEquals(1, wordSuggestions.calls)
    }

    @Test
    fun aFailingSuggestionSource_givesNoSuggestionsInsteadOfAnError_andIsNotCached() = runBlocking {
        wordSuggestions.block = { throw IOException("down") }
        assertEquals(emptyList<String>(), repo.suggest("en", SearchSource.WORDS))

        wordSuggestions.block = { throw http(429) }
        assertEquals(emptyList<String>(), repo.suggest("ent", SearchSource.WORDS))

        wordSuggestions.block = { listOf("entropy") }
        assertEquals(listOf("entropy"), repo.suggest("en", SearchSource.WORDS))
        assertEquals(3, wordSuggestions.calls)
    }

    @Test
    fun loadSummary_ofACompleteResult_needsNoRequest() = runBlocking {
        val complete = sampleConceptResult()

        assertEquals(SearchOutcome.Success(listOf(complete)), repo.loadSummary(complete))
        assertEquals(0, concepts.summaryCalls)
    }
}