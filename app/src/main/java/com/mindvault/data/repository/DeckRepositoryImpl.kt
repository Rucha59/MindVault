package com.mindvault.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.dao.DeckDao
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.mapper.toDomain
import com.mindvault.domain.error.DefaultDeckProtectedException
import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.Deck
import com.mindvault.domain.repository.DeckRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DeckRepositoryImpl @Inject constructor(
    private val dao: DeckDao,
) : DeckRepository {

    override fun observeAll(): Flow<List<Deck>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Deck?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Deck? = dao.getById(id)?.toDomain()

    override suspend fun create(name: String): Deck {
        val now = nowMillis()
        val entity = DeckEntity(
            id = newId(),
            name = name.trim(),
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )
        return try {
            dao.insert(entity)
            entity.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(entity.name)
        }
    }

    override suspend fun rename(id: String, name: String): Deck? {
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(name = name.trim(), updatedAt = nowMillis())
        return try {
            dao.update(updated)
            updated.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(updated.name)
        }
    }

    override suspend fun delete(id: String) {
        val deck = dao.getById(id) ?: return
        if (deck.isDefault) throw DefaultDeckProtectedException()
        dao.deleteById(id)
    }
}