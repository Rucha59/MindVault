package com.mindvault.domain.usecase

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
    fun keysAreLowercaseTrimmedTitles() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(savedItem("1", "  Hello ")))
        assertEquals(mapOf("hello" to "1"), ObserveSavedTitlesUseCase(repo)().first())
    }

    @Test
    fun duplicateTitles_keepTheFirstItemInRepositoryOrder() = runBlocking {
        // observeAll is newest-first, so the first item is the most recently updated.
        val repo = FakeKnowledgeRepository(listOf(savedItem("new", "Run"), savedItem("old", "run")))
        assertEquals(mapOf("run" to "new"), ObserveSavedTitlesUseCase(repo)().first())
    }

    @Test
    fun reflectsLaterChanges() = runBlocking {
        val repo = FakeKnowledgeRepository()
        val flow = ObserveSavedTitlesUseCase(repo)()
        assertTrue(flow.first().isEmpty())

        repo.items.value = listOf(savedItem("1", "Hello"))
        assertEquals(mapOf("hello" to "1"), flow.first())

        repo.delete("1")
        assertTrue(flow.first().isEmpty())
    }
}