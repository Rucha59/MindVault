package com.mindvault.data.repository

import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSource
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Search V1 (Free Dictionary, words only). */
class SearchRepositoryImplTest {

    private class FakeDataSource(private val block: suspend (String) -> List<SearchResult>) : DefinitionDataSource {
        override suspend fun lookup(query: String): List<SearchResult> = block(query)
    }

    private fun repo(block: suspend (String) -> List<SearchResult>) =
        SearchRepositoryImpl(FakeDataSource(block))

    private val sample = SearchResult(
        title = "word",
        pronunciation = null,
        meanings = listOf(SearchMeaning("noun", listOf(SearchDefinition("a unit of language")))),
        sourceName = "Test",
    )

    @Test
    fun emptyList_isNoResults() = runBlocking {
        assertEquals(SearchOutcome.NoResults, repo { emptyList() }.search("word", SearchSource.WORDS))
    }

    @Test
    fun results_areReturnedAsSuccess() = runBlocking {
        assertEquals(
            SearchOutcome.Success(listOf(sample)),
            repo { listOf(sample) }.search("word", SearchSource.WORDS),
        )
    }

    @Test
    fun ioException_isOffline() = runBlocking {
        assertEquals(SearchOutcome.Offline, repo { throw IOException("no network") }.search("word", SearchSource.WORDS))
    }

    @Test
    fun otherException_isFailure() = runBlocking {
        assertEquals(
            SearchOutcome.Failure,
            repo { throw IllegalStateException("bad body") }.search("word", SearchSource.WORDS),
        )
    }

    @Test
    fun suggestions_doNotExistInV1() = runBlocking {
        assertEquals(emptyList<String>(), repo { listOf(sample) }.suggest("wo", SearchSource.WORDS))
    }

    @Test
    fun conceptSource_hasNoResultsInV1_andNeverCallsTheProvider() = runBlocking {
        var called = false
        val outcome = repo { called = true; listOf(sample) }.search("word", SearchSource.CONCEPTS)
        assertEquals(SearchOutcome.NoResults, outcome)
        assertEquals(false, called)
    }
}