package com.mindvault.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import com.mindvault.data.mapper.toDomain
import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.Category
import com.mindvault.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val active: ActiveDatabase,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        active.observe { db -> db.categoryDao().observeAll() }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Category? =
        active.current.categoryDao().getById(id)?.toDomain()

    override suspend fun create(name: String): Category {
        val db = active.current
        val dao = db.categoryDao()
        val trimmed = name.trim()
        dao.findByName(trimmed)?.let { return it.toDomain() }
        val now = nowMillis()
        val entity = CategoryEntity(id = newId(), name = trimmed, createdAt = now, updatedAt = now)
        return try {
            db.withTransaction {
                dao.insert(entity)
                db.syncOutboxDao().upsert(
                    outboxEntry(SyncEntityType.CATEGORY, entity.id, SyncOperation.UPSERT, entity.updatedAt),
                )
            }
            entity.toDomain()
        } catch (e: SQLiteConstraintException) {
            // Lost a race with another insert of the same name: return the winner. Nothing was saved.
            dao.findByName(trimmed)?.toDomain() ?: throw e
        }
    }

    override suspend fun rename(id: String, name: String): Category? {
        val db = active.current
        val dao = db.categoryDao()
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(name = name.trim(), updatedAt = nextUpdatedAt(existing.updatedAt))
        return try {
            db.withTransaction {
                dao.update(updated)
                db.syncOutboxDao().upsert(
                    outboxEntry(SyncEntityType.CATEGORY, id, SyncOperation.UPSERT, updated.updatedAt),
                )
            }
            updated.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(updated.name)
        }
    }

    /**
     * Items in the category are kept; their category becomes null (database rule). Only the category's deletion
     * is recorded for sync: the cloud clears the category on those items too, without touching their updatedAt.
     */
    override suspend fun delete(id: String) {
        val db = active.current
        db.withTransaction {
            val dao = db.categoryDao()
            if (dao.getById(id) == null) return@withTransaction
            dao.deleteById(id)
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.CATEGORY, id, SyncOperation.DELETE, nowMillis()),
            )
        }
    }
}