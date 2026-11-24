package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestSearchTermsUseCaseTest {

    private val repository = FakeSearchRepository()
    private val useCase = SuggestSearchTermsUseCase(repository)
    private val all = SearchScope.ALL.sources

    @Test
    fun fewerThanTwoCharacters_asksNoSource() = runBlocking {
        assertTrue(useCase("e", all).isEmpty())
        assertTrue(useCase("  ", all).isEmpty())
        assertTrue(repository.suggestCalls.isEmpty())
    }

    @Test
    fun overlongQuery_asksNoSource() = runBlocking {
        assertTrue(useCase("a".repeat(SearchLimits.MAX_QUERY_LENGTH + 1), all).isEmpty())
        assertTrue(repository.suggestCalls.isEmpty())
    }

    @Test
    fun queryIsTrimmedAndWhitespaceCollapsed() = runBlocking {
        useCase("  ice    cr ", setOf(SearchSource.WORDS))
        assertEquals(listOf("ice cr"), repository.suggestCalls.map { it.first })
    }

    @Test
    fun onlyTheRequestedSourcesAreAsked() = runBlocking {
        useCase("en", SearchScope.WORDS.sources)
        assertEquals(listOf(SearchSource.WORDS), repository.suggestCalls.map { it.second })

        repository.suggestCalls.clear()
        useCase("en", SearchScope.CONCEPTS.sources)
        assertEquals(listOf(SearchSource.CONCEPTS), repository.suggestCalls.map { it.second })
    }

    @Test
    fun suggestionsOfBothSourcesAreInterleaved_andDuplicatesRemovedIgnoringCase() = runBlocking {
        repository.suggestions[SearchSource.WORDS] = listOf("entropy", "entry", "entrance")
        repository.suggestions[SearchSource.CONCEPTS] = listOf("Entropy", "Enthalpy")

        assertEquals(
            listOf("entropy", "entry", "Enthalpy", "entrance"),
            useCase("en", all),
        )
    }

    @Test
    fun oneSourceCannotPushOutTheOther_andTheListIsCapped() = runBlocking {
        repository.suggestions[SearchSource.WORDS] = (1..6).map { "word$it" }
        repository.suggestions[SearchSource.CONCEPTS] = (1..6).map { "Concept $it" }

        val result = useCase("wo", all)

        assertEquals(SearchLimits.MAX_SUGGESTIONS, result.size)
        assertEquals(listOf("word1", "Concept 1", "word2", "Concept 2", "word3", "Concept 3"), result)
    }

    @Test
    fun anExactMatchComesFirst() = runBlocking {
        repository.suggestions[SearchSource.WORDS] = listOf("entropy", "ent")

        assertEquals(listOf("ent", "entropy"), useCase("ENT", setOf(SearchSource.WORDS)))
    }

    @Test
    fun aSourceWithNoSuggestions_doesNotHideTheOther() = runBlocking {
        repository.suggestions[SearchSource.CONCEPTS] = listOf("Entropy")

        assertEquals(listOf("Entropy"), useCase("en", all))
    }
}