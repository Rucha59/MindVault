package com.mindvault.domain.usecase

import com.mindvault.domain.model.Category
import com.mindvault.domain.model.CreateCategoryResult
import com.mindvault.domain.model.KnowledgeField
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SaveKnowledgeResult
import com.mindvault.domain.model.ValidationError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.mindvault.domain.model.UpdateKnowledgeResult

class UseCasesTest {

    private fun draft(title: String = "Ephemeral", definition: String = "Short-lived") =
        KnowledgeItemDraft(title = title, type = KnowledgeType.WORD, definition = definition)

    private fun errorsOf(result: SaveKnowledgeResult) = (result as SaveKnowledgeResult.Invalid).errors

    @Test
    fun save_blankTitle_isInvalid_andNothingIsSaved() = runBlocking {
        val repo = FakeKnowledgeRepository()
        val result = SaveKnowledgeItemUseCase(repo)(draft(title = "   "))
        assertEquals(ValidationError.BLANK, errorsOf(result)[KnowledgeField.TITLE])
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun save_blankDefinition_isInvalid() = runBlocking {
        val result = SaveKnowledgeItemUseCase(FakeKnowledgeRepository())(draft(definition = ""))
        assertEquals(ValidationError.BLANK, errorsOf(result)[KnowledgeField.DEFINITION])
    }

    @Test
    fun save_titleOverLimit_isTooLong() = runBlocking {
        val title = "a".repeat(SaveKnowledgeItemUseCase.MAX_TITLE_LENGTH + 1)
        val result = SaveKnowledgeItemUseCase(FakeKnowledgeRepository())(draft(title = title))
        assertEquals(ValidationError.TOO_LONG, errorsOf(result)[KnowledgeField.TITLE])
    }

    @Test
    fun save_validDraft_isSavedOnce() = runBlocking {
        val repo = FakeKnowledgeRepository()
        val result = SaveKnowledgeItemUseCase(repo)(draft())
        assertTrue(result is SaveKnowledgeResult.Success)
        assertEquals(1, repo.created.size)
    }

    @Test
    fun createCategory_blank_isInvalid() = runBlocking {
        val result = CreateCategoryUseCase(FakeCategoryRepository())("  ")
        assertEquals(CreateCategoryResult.Invalid(ValidationError.BLANK), result)
    }

    @Test
    fun createCategory_sameNameTwice_returnsExisting() = runBlocking {
        val useCase = CreateCategoryUseCase(FakeCategoryRepository())
        val first = (useCase("Science") as CreateCategoryResult.Created).category
        val second = (useCase("science") as CreateCategoryResult.Created).category
        assertEquals(first.id, second.id)
    }

    @Test
    fun vaultItems_resolveCategoryNames() = runBlocking {
        fun item(id: String, categoryId: String?) = KnowledgeItem(
            id, "T$id", KnowledgeType.WORD, "D", "", null, categoryId, 0L, 0L,
        )
        val useCase = ObserveVaultItemsUseCase(
            FakeKnowledgeRepository(listOf(item("1", "c1"), item("2", null))),
            FakeCategoryRepository(listOf(Category("c1", "Science", 0L))),
        )
        val entries = useCase().first()
        assertEquals("Science", entries[0].categoryName)
        assertNull(entries[1].categoryName)
    }
    @Test
    fun detail_resolvesCategoryName_andIsNullWhenMissing() = runBlocking {
        val item = KnowledgeItem("1", "T", KnowledgeType.WORD, "D", "", null, "c1", 0L, 0L)
        val useCase = ObserveKnowledgeDetailUseCase(
            FakeKnowledgeRepository(listOf(item)),
            FakeCategoryRepository(listOf(Category("c1", "Science", 0L))),
        )
        assertEquals("Science", useCase("1").first()?.categoryName)
        assertNull(useCase("missing").first())
    }
    private fun existing(title: String = "Old") =
        KnowledgeItem("1", title, KnowledgeType.WORD, "D", "", null, null, 0L, 0L)

    @Test
    fun update_validEdit_isStored() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(existing()))
        val result = UpdateKnowledgeItemUseCase(repo)(existing(title = "New"))
        assertTrue(result is UpdateKnowledgeResult.Updated)
        assertEquals("New", repo.items.value.first().title)
    }

    @Test
    fun update_blankTitle_isInvalid_andNothingChanges() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(existing()))
        val result = UpdateKnowledgeItemUseCase(repo)(existing(title = " "))
        val errors = (result as UpdateKnowledgeResult.Invalid).errors
        assertEquals(ValidationError.BLANK, errors[KnowledgeField.TITLE])
        assertEquals("Old", repo.items.value.first().title)
    }

    @Test
    fun update_missingItem_isNotFound() = runBlocking {
        val result = UpdateKnowledgeItemUseCase(FakeKnowledgeRepository())(existing())
        assertEquals(UpdateKnowledgeResult.NotFound, result)
    }

    @Test
    fun delete_removesTheItem() = runBlocking {
        val repo = FakeKnowledgeRepository(listOf(existing()))
        DeleteKnowledgeItemUseCase(repo)("1")
        assertTrue(repo.items.value.isEmpty())
    }
}