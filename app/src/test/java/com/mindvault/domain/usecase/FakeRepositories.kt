package com.mindvault.domain.usecase

import com.mindvault.domain.model.Category
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.repository.CategoryRepository
import com.mindvault.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeKnowledgeRepository(initial: List<KnowledgeItem> = emptyList()) : KnowledgeRepository {
    val items = MutableStateFlow(initial)
    val created = mutableListOf<KnowledgeItemDraft>()

    override fun observeAll(): Flow<List<KnowledgeItem>> = items
    override fun observeById(id: String): Flow<KnowledgeItem?> =
        items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getById(id: String): KnowledgeItem? = items.value.firstOrNull { it.id == id }
    override suspend fun findByTitle(title: String): KnowledgeItem? =
        items.value.firstOrNull { it.title.equals(title, ignoreCase = true) }

    override suspend fun create(draft: KnowledgeItemDraft): KnowledgeItem {
        created += draft
        val item = KnowledgeItem(
            id = "id-${created.size}", title = draft.title, type = draft.type,
            definition = draft.definition, personalNotes = draft.personalNotes,
            example = draft.example, categoryId = draft.categoryId, createdAt = 0L, updatedAt = 0L,
        )
        items.value = items.value + item
        return item
    }

    override suspend fun update(item: KnowledgeItem): KnowledgeItem? {
        if (items.value.none { it.id == item.id }) return null
        items.value = items.value.map { if (it.id == item.id) item else it }
        return item
    }
    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val categories = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Category>> = categories
    override suspend fun getById(id: String): Category? = categories.value.firstOrNull { it.id == id }

    override suspend fun create(name: String): Category {
        categories.value.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        val category = Category(id = "c-$name", name = name, createdAt = 0L)
        categories.value = categories.value + category
        return category
    }

    override suspend fun rename(id: String, name: String): Category? = null
    override suspend fun delete(id: String) {
        categories.value = categories.value.filterNot { it.id == id }
    }
}