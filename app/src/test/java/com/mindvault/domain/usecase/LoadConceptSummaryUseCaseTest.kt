package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadConceptSummaryUseCaseTest {

    private val repository = FakeSearchRepository()
    private val useCase = LoadConceptSummaryUseCase(repository)

    @Test
    fun conceptWithoutSummary_isLoadedFromTheRepository() = runBlocking {
        val outcome = useCase(sampleConceptResult(summary = null)) as SearchOutcome.Success

        assertEquals("Loaded summary of Entropy.", outcome.results.single().summary)
        assertEquals(1, repository.summaryCalls.size)
    }

    @Test
    fun conceptWithASummary_isReturnedAsIsWithoutCallingTheRepository() = runBlocking {
        val concept = sampleConceptResult()

        assertEquals(SearchOutcome.Success(listOf(concept)), useCase(concept))
        assertTrue(repository.summaryCalls.isEmpty())
    }

    @Test
    fun aWord_isReturnedAsIs() = runBlocking {
        val word = sampleSearchResult()

        assertEquals(SearchOutcome.Success(listOf(word)), useCase(word))
        assertTrue(repository.summaryCalls.isEmpty())
    }

    @Test
    fun repositoryFailures_passThrough() = runBlocking {
        repository.summaryOutcome = SearchOutcome.Offline

        assertEquals(SearchOutcome.Offline, useCase(sampleConceptResult(summary = null)))
    }
}