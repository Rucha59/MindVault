package com.mindvault.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.dao.CategoryDao
import com.mindvault.data.local.entity.CategoryEntity
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
    private val dao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Category? = dao.getById(id)?.toDomain()

    override suspend fun create(name: String): Category {
        val trimmed = name.trim()
        dao.findByName(trimmed)?.let { return it.toDomain() }
        val entity = CategoryEntity(id = newId(), name = trimmed, createdAt = nowMillis())
        return try {
            dao.insert(entity)
            entity.toDomain()
        } catch (e: SQLiteConstraintException) {
            // Lost a race with another insert of the same name: return the winner.
            dao.findByName(trimmed)?.toDomain() ?: throw e
        }
    }

    override suspend fun rename(id: String, name: String): Category? {
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(name = name.trim())
        return try {
            dao.update(updated)
            updated.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(updated.name)
        }
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}