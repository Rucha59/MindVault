package com.mindvault.data.repository

import androidx.room.withTransaction
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
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
    private val active: ActiveDatabase,
) : KnowledgeRepository {

    override fun observeAll(): Flow<List<KnowledgeItem>> =
        active.observe { db -> db.knowledgeItemDao().observeAll() }
            .map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<KnowledgeItem?> =
        active.observe { db -> db.knowledgeItemDao().observeById(id) }
            .map { it?.toDomain() }

    override suspend fun getById(id: String): KnowledgeItem? =
        active.current.knowledgeItemDao().getById(id)?.toDomain()

    override suspend fun findByTitle(title: String): KnowledgeItem? =
        active.current.knowledgeItemDao().findByTitle(title.trim())?.toDomain()

    override suspend fun create(draft: KnowledgeItemDraft): KnowledgeItem {
        val db = active.current
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
        // The change and its outbox entry are saved together or not at all.
        db.withTransaction {
            db.knowledgeItemDao().insert(item.toEntity())
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, item.id, SyncOperation.UPSERT, item.updatedAt),
            )
        }
        return item
    }

    override suspend fun update(item: KnowledgeItem): KnowledgeItem? {
        val db = active.current
        return db.withTransaction<KnowledgeItem?> {
            val dao = db.knowledgeItemDao()
            val existing = dao.getById(item.id) ?: return@withTransaction null
            val updated = item.copy(
                createdAt = existing.createdAt,
                updatedAt = nextUpdatedAt(existing.updatedAt),
            ).normalized()
            dao.update(updated.toEntity())
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, updated.id, SyncOperation.UPSERT, updated.updatedAt),
            )
            updated
        }
    }

    /**
     * Also removes the item's flashcards and review state (database cascade). Only the item's deletion is
     * recorded for sync: the cloud removes its cards and review states with it.
     */
    override suspend fun delete(id: String) {
        val db = active.current
        db.withTransaction {
            val dao = db.knowledgeItemDao()
            if (dao.getById(id) == null) return@withTransaction
            dao.deleteById(id)
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, id, SyncOperation.DELETE, nowMillis()),
            )
        }
    }

    private fun KnowledgeItem.normalized() = copy(
        title = title.trim(),
        definition = definition.trim(),
        personalNotes = personalNotes.trim(),
        example = example?.trim()?.takeIf { it.isNotEmpty() },
    )
}