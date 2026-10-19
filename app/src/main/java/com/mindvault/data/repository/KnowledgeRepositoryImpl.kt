package com.mindvault.data.repository

import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.dao.KnowledgeItemDao
import com.mindvault.data.mapper.toDomain
import com.mindvault.data.mapper.toEntity
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class KnowledgeRepositoryImpl @Inject constructor(
    private val dao: KnowledgeItemDao,
) : KnowledgeRepository {

    override fun observeAll(): Flow<List<KnowledgeItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<KnowledgeItem?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): KnowledgeItem? = dao.getById(id)?.toDomain()

    override suspend fun findByTitle(title: String): KnowledgeItem? =
        dao.findByTitle(title.trim())?.toDomain()

    override suspend fun create(draft: KnowledgeItemDraft): KnowledgeItem {
        val now = nowMillis()
        val item = KnowledgeItem(
            id = newId(),
            title = draft.title,
            type = draft.type,
            definition = draft.definition,
            personalNotes = draft.personalNotes,
            example = draft.example,
            categoryId = draft.categoryId,
            createdAt = now,
            updatedAt = now,
        ).normalized()
        dao.insert(item.toEntity())
        return item
    }

    override suspend fun update(item: KnowledgeItem): KnowledgeItem? {
        val existing = dao.getById(item.id) ?: return null
        val updated = item.copy(createdAt = existing.createdAt, updatedAt = nowMillis()).normalized()
        dao.update(updated.toEntity())
        return updated
    }

    override suspend fun delete(id: String) = dao.deleteById(id)

    private fun KnowledgeItem.normalized() = copy(
        title = title.trim(),
        definition = definition.trim(),
        personalNotes = personalNotes.trim(),
        example = example?.trim()?.takeIf { it.isNotEmpty() },
    )
}