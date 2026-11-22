package com.mindvault.domain.usecase

import com.mindvault.domain.model.Category
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.RenameCategoryResult
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryManagementUseCasesTest {

    private class FakeDeckRepository(decks: List<Deck> = emptyList()) : DeckRepository {
        val decks = MutableStateFlow(decks)
        val deleted = mutableListOf<String>()

        override fun observeAll(): Flow<List<Deck>> = decks
        override fun observeById(id: String): Flow<Deck?> = decks.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun getById(id: String): Deck? = decks.value.firstOrNull { it.id == id }
        override suspend fun create(name: String): Deck = error("unused")
        override suspend fun rename(id: String, name: String): Deck? = error("unused")
        override suspend fun delete(id: String) {
            deleted += id
            decks.value = decks.value.filterNot { it.id == id }
        }
        override suspend fun ensureCategoryDeck(categoryId: String): Deck? =
            decks.value.firstOrNull { it.categoryId == categoryId }
        override suspend fun findCategoryDeck(categoryId: String): Deck? =
            decks.value.firstOrNull { it.categoryId == categoryId }
        override suspend fun findCategoryDeckHoldingItem(itemId: String, categoryId: String): Deck? = error("unused")
    }

    private fun deck(id: String, categoryId: String?, createdAt: Long = 0L) =
        Deck(id = id, name = "Deck $id", isDefault = false, createdAt = createdAt, updatedAt = createdAt, categoryId = categoryId)

    private fun category(id: String, name: String) = Category(id = id, name = name, createdAt = 0L)

    // ---- rename

    @Test
    fun rename_blank_isInvalid_andNothingChanges() = runBlocking {
        val repo = FakeCategoryRepository(listOf(category("c1", "Science")))
        val result = RenameCategoryUseCase(repo)("c1", "   ")
        assertEquals(RenameCategoryResult.Invalid(ValidationError.BLANK), result)
        assertEquals("Science", repo.categories.value.single().name)
    }

    @Test
    fun rename_tooLong_isInvalid() = runBlocking {
        val repo = FakeCategoryRepository(listOf(category("c1", "Science")))
        val result = RenameCategoryUseCase(repo)("c1", "a".repeat(CreateCategoryUseCase.MAX_NAME_LENGTH + 1))
        assertEquals(RenameCategoryResult.Invalid(ValidationError.TOO_LONG), result)
    }

    @Test
    fun rename_takenName_isDuplicate() = runBlocking {
        val repo = FakeCategoryRepository(listOf(category("c1", "Science"), category("c2", "Languages")))
        assertEquals(RenameCategoryResult.DuplicateName, RenameCategoryUseCase(repo)("c2", "science"))
    }

    @Test
    fun rename_valid_isStoredTrimmed() = runBlocking {
        val repo = FakeCategoryRepository(listOf(category("c1", "Science")))
        val result = RenameCategoryUseCase(repo)("c1", "  Natural science ")
        assertEquals("Natural science", (result as RenameCategoryResult.Renamed).category.name)
        assertEquals("Natural science", repo.categories.value.single().name)
    }

    @Test
    fun rename_missingCategory_isNotFound() = runBlocking {
        val result = RenameCategoryUseCase(FakeCategoryRepository())("missing", "Name")
        assertEquals(RenameCategoryResult.NotFound, result)
    }

    // ---- delete

    @Test
    fun delete_keepingTheDeck_leavesTheDeckAlone() = runBlocking {
        val categories = FakeCategoryRepository(listOf(category("c1", "Science")))
        val decks = FakeDeckRepository(listOf(deck("d1", "c1")))

        DeleteCategoryUseCase(categories, decks)("c1", deleteDeck = false)

        assertTrue(categories.categories.value.isEmpty())
        assertTrue(decks.deleted.isEmpty())
        assertEquals(1, decks.decks.value.size)
    }

    @Test
    fun delete_withTheDeck_deletesTheCategoryAndItsDeck() = runBlocking {
        val categories = FakeCategoryRepository(listOf(category("c1", "Science")))
        val decks = FakeDeckRepository(listOf(deck("d1", "c1"), deck("d2", null)))

        DeleteCategoryUseCase(categories, decks)("c1", deleteDeck = true)

        assertTrue(categories.categories.value.isEmpty())
        assertEquals(listOf("d1"), decks.deleted)
        assertEquals(listOf("d2"), decks.decks.value.map { it.id }) // other decks are never touched
    }

    @Test
    fun delete_withTheDeck_whenTheCategoryHasNone_justDeletesTheCategory() = runBlocking {
        val categories = FakeCategoryRepository(listOf(category("c1", "Science")))
        val decks = FakeDeckRepository(listOf(deck("d2", null)))

        DeleteCategoryUseCase(categories, decks)("c1", deleteDeck = true)

        assertTrue(categories.categories.value.isEmpty())
        assertTrue(decks.deleted.isEmpty())
    }

    // ---- category decks

    @Test
    fun categoryDecks_picksTheOldestPerCategory_andIgnoresUnlinkedDecks() = runBlocking {
        val decks = FakeDeckRepository(
            listOf(
                deck("late", "c1", createdAt = 20),
                deck("early", "c1", createdAt = 10),
                deck("other", "c2", createdAt = 5),
                deck("plain", null, createdAt = 1),
            ),
        )

        val byCategory = ObserveCategoryDecksUseCase(decks)().first()

        assertEquals(setOf("c1", "c2"), byCategory.keys)
        assertEquals("early", byCategory.getValue("c1").id)
        assertNull(byCategory["c3"])
    }
}