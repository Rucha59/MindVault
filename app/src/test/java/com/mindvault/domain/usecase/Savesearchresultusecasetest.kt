package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SaveSearchResult
import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveSearchResultUseCaseTest {

    @Test
    fun savesANewWordWithoutACategory() = runBlocking {
        val repo = FakeKnowledgeRepository()

        val outcome = newSaveSearchResultUseCase(repo)(sampleSearchResult())

        assertEquals(SaveSearchResult.Saved, outcome)
        val draft = repo.created.single()
        assertEquals("hello", draft.title)
        assertEquals(KnowledgeType.WORD, draft.type)
        assertEquals("(noun) used as a greeting", draft.definition) // no source link, so no source line
        assertEquals("hello there", draft.example)
        assertEquals("", draft.personalNotes)
        assertNull(draft.categoryId)
    }

    @Test
    fun savesAConceptAsAConcept_withoutASourceLine() = runBlocking {
        val repo = FakeKnowledgeRepository()

        val outcome = newSaveSearchResultUseCase(repo)(sampleConceptResult())

        assertEquals(SaveSearchResult.Saved, outcome)
        val draft = repo.created.single()
        assertEquals("Entropy", draft.title)
        assertEquals(KnowledgeType.CONCEPT, draft.type)
        assertEquals("A thermodynamic state variable.", draft.definition)
        assertNull(draft.example)
        assertNull(draft.categoryId)
    }

    @Test
    fun alreadySavedWord_isNotSavedAgain_ignoringCase() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("existing", "Hello")))

        val outcome = newSaveSearchResultUseCase(repo)(sampleSearchResult("hello"))

        assertEquals(SaveSearchResult.AlreadySaved("existing"), outcome)
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun aSavedWord_doesNotBlockSavingTheConceptWithTheSameTitle() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("w", "Entropy", KnowledgeType.WORD)))

        val outcome = newSaveSearchResultUseCase(repo)(sampleConceptResult("Entropy"))

        assertEquals(SaveSearchResult.Saved, outcome)
        assertEquals(KnowledgeType.CONCEPT, repo.created.single().type)
    }

    @Test
    fun aSavedConcept_isFlaggedAlreadySavedForTheConceptOnly() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("c", "Entropy", KnowledgeType.CONCEPT)))

        assertEquals(
            SaveSearchResult.AlreadySaved("c"),
            newSaveSearchResultUseCase(repo)(sampleConceptResult("entropy")),
        )
        assertEquals(SaveSearchResult.Saved, newSaveSearchResultUseCase(repo)(sampleSearchResult("Entropy")))
    }

    @Test
    fun conceptWithoutItsSummary_isInvalidAndNothingIsSaved() = runBlocking {
        val repo = FakeKnowledgeRepository()

        val outcome = newSaveSearchResultUseCase(repo)(sampleConceptResult(summary = null))

        assertEquals(SaveSearchResult.Invalid, outcome)
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun overlongDefinition_isInvalidAndNothingIsSaved() = runBlocking {
        val repo = FakeKnowledgeRepository()
        val huge = sampleSearchResult().copy(
            meanings = listOf(
                SearchMeaning("noun", listOf(SearchDefinition("x".repeat(SaveKnowledgeItemUseCase.MAX_DEFINITION_LENGTH + 1)))),
            ),
        )

        val outcome = newSaveSearchResultUseCase(repo)(huge)

        assertEquals(SaveSearchResult.Invalid, outcome)
        assertTrue(repo.created.isEmpty())
    }
}