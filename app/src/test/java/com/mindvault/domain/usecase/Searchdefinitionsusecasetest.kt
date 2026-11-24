package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDefinitionsUseCaseTest {

    private val repository = FakeSearchRepository()
    private val useCase = SearchDefinitionsUseCase(repository)
    private val words = setOf(SearchSource.WORDS)

    @Test
    fun trimsAndCollapsesWhitespaceBeforeSearching() = runBlocking {
        useCase("  hello    world ", words)
        assertEquals(listOf("hello world"), repository.queries)
    }

    @Test
    fun blankQuery_isNoResultsWithoutCallingAnySource() = runBlocking {
        assertEquals(
            mapOf(SearchSource.WORDS to SearchOutcome.NoResults),
            useCase("   ", words),
        )
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun overlongQuery_isNoResultsWithoutCallingAnySource() = runBlocking {
        val tooLong = "a".repeat(SearchLimits.MAX_QUERY_LENGTH + 1)
        assertEquals(SearchOutcome.NoResults, useCase(tooLong, words).getValue(SearchSource.WORDS))
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun allScope_queriesBothSourcesAndReturnsOneOutcomeEach() = runBlocking {
        repository.outcomes[SearchSource.WORDS] = SearchOutcome.Success(listOf(sampleSearchResult("entropy")))
        repository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Success(listOf(sampleConceptResult("Entropy")))

        val result = useCase("entropy", SearchScope.ALL.sources)

        assertEquals(setOf(SearchSource.WORDS, SearchSource.CONCEPTS), result.keys)
        assertEquals(setOf(SearchSource.WORDS, SearchSource.CONCEPTS), repository.sources.toSet())
    }

    @Test
    fun wordsScope_queriesOnlyTheWordSource() = runBlocking {
        useCase("entropy", SearchScope.WORDS.sources)
        assertEquals(listOf(SearchSource.WORDS), repository.sources)
    }

    @Test
    fun conceptsScope_queriesOnlyTheConceptSource() = runBlocking {
        useCase("entropy", SearchScope.CONCEPTS.sources)
        assertEquals(listOf(SearchSource.CONCEPTS), repository.sources)
    }

    @Test
    fun oneSourceFailing_doesNotHideTheOther() = runBlocking {
        repository.outcomes[SearchSource.WORDS] = SearchOutcome.Success(listOf(sampleSearchResult("entropy")))
        repository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Offline

        val result = useCase("entropy", SearchScope.ALL.sources)

        assertTrue(result.getValue(SearchSource.WORDS) is SearchOutcome.Success)
        assertEquals(SearchOutcome.Offline, result.getValue(SearchSource.CONCEPTS))
    }

    @Test
    fun success_isLimitedToTwoDefinitionsPerMeaning() = runBlocking {
        val many = sampleSearchResult().copy(
            meanings = listOf(
                SearchMeaning("noun", listOf("a", "b", "c").map { SearchDefinition(it) }),
            ),
        )
        repository.outcome = SearchOutcome.Success(listOf(many))

        val outcome = useCase("hello", words).getValue(SearchSource.WORDS) as SearchOutcome.Success

        assertEquals(2, outcome.results.single().meanings.single().definitions.size)
    }

    @Test
    fun offlineBusyAndFailure_passThrough() = runBlocking {
        listOf(SearchOutcome.Offline, SearchOutcome.Busy, SearchOutcome.Failure).forEach { expected ->
            repository.outcome = expected
            assertEquals(expected, useCase("hello", words).getValue(SearchSource.WORDS))
        }
    }
}