package com.mindvault.domain.usecase

import com.mindvault.domain.model.CategoryFilter
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.model.VaultQuery
import com.mindvault.domain.model.VaultSort
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyVaultQueryUseCaseTest {

    private val apply = ApplyVaultQueryUseCase()

    private fun entry(
        id: String,
        title: String,
        type: KnowledgeType = KnowledgeType.WORD,
        definition: String = "def",
        notes: String = "",
        categoryId: String? = null,
        categoryName: String? = null,
        createdAt: Long = 0L,
        updatedAt: Long = 0L,
    ) = VaultEntry(
        KnowledgeItem(id, title, type, definition, notes, null, categoryId, createdAt, updatedAt),
        categoryName,
    )

    private fun ids(entries: List<VaultEntry>) = entries.map { it.item.id }

    private val sample = listOf(
        entry("1", "Ephemeral", definition = "Lasting a very short time", categoryId = "c1", categoryName = "Language"),
        entry("2", "API", KnowledgeType.CONCEPT, "Application Programming Interface", notes = "REST and more"),
        entry("3", "banana", KnowledgeType.OTHER, "A fruit", categoryId = "c2", categoryName = "Food"),
    )

    @Test
    fun blankSearch_returnsEverything() {
        assertEquals(3, apply(sample, VaultQuery(searchText = "   ")).size)
    }

    @Test
    fun search_matchesTitleDefinitionNotesAndCategory_caseInsensitive() {
        assertEquals(listOf("1"), ids(apply(sample, VaultQuery(searchText = "EPHEMERAL"))))
        assertEquals(listOf("2"), ids(apply(sample, VaultQuery(searchText = "programming"))))
        assertEquals(listOf("2"), ids(apply(sample, VaultQuery(searchText = "rest"))))
        assertEquals(listOf("3"), ids(apply(sample, VaultQuery(searchText = "food"))))
    }

    @Test
    fun search_withSeveralWords_requiresAllOfThem() {
        assertEquals(listOf("1"), ids(apply(sample, VaultQuery(searchText = "short time"))))
        assertEquals(emptyList<String>(), ids(apply(sample, VaultQuery(searchText = "short fruit"))))
    }

    @Test
    fun typeFilter_keepsOnlyThatType() {
        val result = apply(sample, VaultQuery(type = KnowledgeType.CONCEPT))
        assertEquals(listOf("2"), ids(result))
    }

    @Test
    fun categoryFilter_specificAndUncategorized() {
        assertEquals(listOf("1"), ids(apply(sample, VaultQuery(category = CategoryFilter.Specific("c1")))))
        assertEquals(listOf("2"), ids(apply(sample, VaultQuery(category = CategoryFilter.Uncategorized))))
    }

    @Test
    fun filters_combine() {
        val query = VaultQuery(searchText = "a", type = KnowledgeType.OTHER, category = CategoryFilter.Specific("c2"))
        assertEquals(listOf("3"), ids(apply(sample, query)))
    }

    @Test
    fun sort_alphabeticalIgnoresCase_andReverses() {
        assertEquals(listOf("2", "3", "1"), ids(apply(sample, VaultQuery(sort = VaultSort.A_TO_Z))))
        assertEquals(listOf("1", "3", "2"), ids(apply(sample, VaultQuery(sort = VaultSort.Z_TO_A))))
    }

    @Test
    fun sort_byRecentActivity() {
        val items = listOf(
            entry("a", "A", createdAt = 1, updatedAt = 30),
            entry("b", "B", createdAt = 3, updatedAt = 10),
            entry("c", "C", createdAt = 2, updatedAt = 20),
        )
        assertEquals(listOf("a", "c", "b"), ids(apply(items, VaultQuery(sort = VaultSort.RECENTLY_UPDATED))))
        assertEquals(listOf("b", "c", "a"), ids(apply(items, VaultQuery(sort = VaultSort.RECENTLY_ADDED))))
    }
}