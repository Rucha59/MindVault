package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDefinitionsUseCaseTest {

    private val repository = FakeSearchRepository()
    private val useCase = SearchDefinitionsUseCase(repository)

    @Test
    fun trimsAndCollapsesWhitespaceBeforeSearching() = runBlocking {
        useCase("  hello    world ")
        assertEquals(listOf("hello world"), repository.queries)
    }

    @Test
    fun blankQuery_isNoResultsWithoutCallingTheProvider() = runBlocking {
        assertEquals(SearchOutcome.NoResults, useCase("   "))
        assertTrue(repository.queries.isEmpty())
    }

    @Test
    fun overlongQuery_isNoResultsWithoutCallingTheProvider() = runBlocking {
        val tooLong = "a".repeat(SearchLimits.MAX_QUERY_LENGTH + 1)
        assertEquals(SearchOutcome.NoResults, useCase(tooLong))
        assertTrue(repository.queries.isEmpty())
    }

    @Test
    fun success_isLimitedToTwoDefinitionsPerMeaning() = runBlocking {
        val many = sampleSearchResult().copy(
            meanings = listOf(
                SearchMeaning("noun", listOf("a", "b", "c").map { SearchDefinition(it) }),
            ),
        )
        repository.outcome = SearchOutcome.Success(listOf(many))

        val outcome = useCase("hello") as SearchOutcome.Success

        assertEquals(2, outcome.results.single().meanings.single().definitions.size)
    }

    @Test
    fun offlineAndFailure_passThrough() = runBlocking {
        repository.outcome = SearchOutcome.Offline
        assertEquals(SearchOutcome.Offline, useCase("hello"))
        repository.outcome = SearchOutcome.Failure
        assertEquals(SearchOutcome.Failure, useCase("hello"))
    }
}