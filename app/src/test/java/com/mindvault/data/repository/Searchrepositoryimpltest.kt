package com.mindvault.data.repository

import com.mindvault.data.remote.DefinitionDataSource
import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals(SearchOutcome.NoResults, repo { emptyList() }.search("word"))
    }

    @Test
    fun results_areReturnedAsSuccess() = runBlocking {
        assertEquals(SearchOutcome.Success(listOf(sample)), repo { listOf(sample) }.search("word"))
    }

    @Test
    fun ioException_isOffline() = runBlocking {
        assertEquals(SearchOutcome.Offline, repo { throw IOException("no network") }.search("word"))
    }

    @Test
    fun otherException_isFailure() = runBlocking {
        assertEquals(SearchOutcome.Failure, repo { throw IllegalStateException("bad body") }.search("word"))
    }
}