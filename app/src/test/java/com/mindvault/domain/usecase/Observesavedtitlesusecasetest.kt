package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveSavedTitlesUseCaseTest {

    @Test
    fun emptyVault_givesEmptyMap() = runBlocking {
        val useCase = ObserveSavedTitlesUseCase(FakeKnowledgeRepository())
        assertTrue(useCase().first().isEmpty())
    }

    @Test
    fun keysAreTypeAndLowercaseTrimmedTitle() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("1", "  Hello ")))
        assertEquals(mapOf("WORD:hello" to "1"), ObserveSavedTitlesUseCase(repo)().first())
    }

    @Test
    fun aWordAndAConceptWithTheSameTitle_haveSeparateKeys() = runBlocking {
        val repo = FakeKnowledgeRepository(
            listOf(savedItem("w", "Entropy", KnowledgeType.WORD), savedItem("c", "entropy", KnowledgeType.CONCEPT)),
        )
        assertEquals(
            mapOf("WORD:entropy" to "w", "CONCEPT:entropy" to "c"),
            ObserveSavedTitlesUseCase(repo)().first(),
        )
    }

    @Test
    fun duplicateTitlesOfTheSameType_keepTheFirstItemInRepositoryOrder() = runBlocking {
        // observeAll is newest-first, so the first item is the most recently updated.
        val repo = FakeKnowledgeRepository(listOf(savedItem("new", "Run"), savedItem("old", "run")))
        assertEquals(mapOf("WORD:run" to "new"), ObserveSavedTitlesUseCase(repo)().first())
    }

    @Test
    fun reflectsLaterChanges() = runBlocking {
        val repo = FakeKnowledgeRepository()
        val flow = ObserveSavedTitlesUseCase(repo)()
        assertTrue(flow.first().isEmpty())

        repo.items.value = listOf(savedItem("1", "Hello"))
        assertEquals(mapOf("WORD:hello" to "1"), flow.first())

        repo.delete("1")
        assertTrue(flow.first().isEmpty())
    }
}