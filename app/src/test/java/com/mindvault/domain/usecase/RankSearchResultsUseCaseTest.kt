package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchResultKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankSearchResultsUseCaseTest {

    private fun rank(query: String, results: List<SearchResult>): List<SearchResult> =
        RankSearchResultsUseCase().invoke(query, results)

    private fun concept(title: String) = sampleConceptResult(title, summary = null)
    private fun label(r: SearchResult) = (if (r.kind == SearchResultKind.WORD) "W:" else "C:") + r.title

    @Test
    fun exactMatchesFirst_thenPrefix_thenContains_thenTheRest() {
        val results = listOf(
            concept("Second law of thermodynamics"),
            concept("Cross-entropy"),
            concept("Entropy (information theory)"),
            concept("Entropy"),
        )

        assertEquals(
            listOf("Entropy", "Entropy (information theory)", "Cross-entropy", "Second law of thermodynamics"),
            rank("entropy", results).map { it.title },
        )
    }

    @Test
    fun aWordAndAConceptWithTheSameTitle_bothShow_wordFirst() {
        val results = listOf(concept("Entropy"), sampleSearchResult("entropy"))

        assertEquals(listOf("W:entropy", "C:Entropy"), rank("Entropy", results).map(::label))
    }

    @Test
    fun exactMatchesOfBothKinds_comeBeforeEverythingElse() {
        val results = listOf(
            concept("Entropy (information theory)"),
            concept("Entropy"),
            sampleSearchResult("entropy"),
        )

        assertEquals(
            listOf("W:entropy", "C:Entropy", "C:Entropy (information theory)"),
            rank("entropy", results).map(::label),
        )
    }

    @Test
    fun sourceOrderIsKeptInsideATier() {
        val results = listOf(concept("Zeta entropy"), concept("Alpha entropy"))

        assertEquals(listOf("Zeta entropy", "Alpha entropy"), rank("entropy", results).map { it.title })
    }

    @Test
    fun duplicatesOfTheSameKindAndTitle_areRemoved_keepingTheFirst() {
        val first = concept("Entropy")
        val duplicate = sampleConceptResult("entropy", summary = "later copy")

        val ranked = rank("entropy", listOf(first, duplicate))

        assertEquals(listOf(first), ranked)
    }

    @Test
    fun matchingIgnoresCaseAndExtraWhitespace() {
        val results = listOf(concept("Cross-entropy"), concept("Ice cream"))

        assertEquals(
            listOf("Ice cream", "Cross-entropy"),
            rank("  ICE   Cream ", results).map { it.title },
        )
    }

    @Test
    fun blankQuery_keepsTheOriginalOrder() {
        val results = listOf(concept("B"), concept("A"))
        assertEquals(listOf("B", "A"), rank("   ", results).map { it.title })
    }

    @Test
    fun emptyInput_givesEmptyOutput() {
        assertTrue(rank("entropy", emptyList()).isEmpty())
    }
}