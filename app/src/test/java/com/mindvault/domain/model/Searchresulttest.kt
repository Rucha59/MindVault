package com.mindvault.domain.model

import com.mindvault.domain.usecase.sampleConceptResult
import com.mindvault.domain.usecase.sampleSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultTest {

    @Test
    fun titleKey_isTrimmedAndLowercase() {
        assertEquals("hello", "  HeLLo ".toTitleKey())
    }

    @Test
    fun savedItemKey_separatesAWordFromAConceptWithTheSameTitle() {
        assertEquals("WORD:entropy", sampleSearchResult("Entropy").savedItemKey())
        assertEquals("CONCEPT:entropy", sampleConceptResult("Entropy").savedItemKey())
        assertNotEquals(sampleSearchResult("Entropy").savedItemKey(), sampleConceptResult("Entropy").savedItemKey())
    }

    @Test
    fun limited_keepsFourMeaningsAndTwoDefinitionsEach() {
        val many = sampleSearchResult().copy(
            meanings = (1..6).map { n ->
                SearchMeaning("pos$n", (1..5).map { SearchDefinition("d$it") })
            },
        )
        val shown = many.limited()
        assertEquals(SearchLimits.MAX_MEANINGS, shown.meanings.size)
        assertTrue(shown.meanings.all { it.definitions.size == SearchLimits.MAX_DEFINITIONS_PER_MEANING })
    }

    @Test
    fun wordSaveContent_listsDefinitionsWithPartOfSpeechAndTakesTheFirstExample() {
        val content = sampleSearchResult().toSaveContent()
        assertEquals("hello", content.title)
        assertEquals("(noun) used as a greeting", content.definition)
        assertEquals("hello there", content.example)
        assertEquals(KnowledgeType.WORD, content.type)
    }

    @Test
    fun theSourceIsAFootnoteOnTheCardOnly_neverPartOfTheSavedText() {
        val word = sampleSearchResult().copy(
            sourceName = "Wiktionary",
            sourceUrl = "https://en.wiktionary.org/wiki/hello",
            license = "CC BY-SA 4.0",
        )
        val content = word.toSaveContent()

        assertEquals("(noun) used as a greeting", content.definition)
        assertFalse(content.definition.contains("Source"))
        assertFalse(content.definition.contains("wiktionary.org"))
    }

    @Test
    fun conceptSaveContent_isJustTheSummary_asAConcept() {
        val content = sampleConceptResult().toSaveContent()
        assertEquals(KnowledgeType.CONCEPT, content.type)
        assertEquals("A thermodynamic state variable.", content.definition)
        assertNull(content.example)
    }

    @Test
    fun needsSummary_isOnlyTrueForAConceptWithoutOne() {
        assertTrue(sampleConceptResult(summary = null).needsSummary)
        assertTrue(sampleConceptResult(summary = "  ").needsSummary)
        assertFalse(sampleConceptResult().needsSummary)
        assertFalse(sampleSearchResult().needsSummary)
    }
}