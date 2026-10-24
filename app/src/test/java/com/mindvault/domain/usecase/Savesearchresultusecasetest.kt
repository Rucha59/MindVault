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
    fun savesANewResultAsAWordWithoutACategory() = runBlocking {
        val repo = FakeKnowledgeRepository()

        val outcome = newSaveSearchResultUseCase(repo)(sampleSearchResult())

        assertEquals(SaveSearchResult.Saved, outcome)
        val draft = repo.created.single()
        assertEquals("hello", draft.title)
        assertEquals(KnowledgeType.WORD, draft.type)
        assertEquals("(noun) used as a greeting", draft.definition)
        assertEquals("hello there", draft.example)
        assertEquals("", draft.personalNotes)
        assertNull(draft.categoryId)
    }

    @Test
    fun alreadySavedTitle_isNotSavedAgain_ignoringCase() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("existing", "Hello")))

        val outcome = newSaveSearchResultUseCase(repo)(sampleSearchResult("hello"))

        assertEquals(SaveSearchResult.AlreadySaved("existing"), outcome)
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