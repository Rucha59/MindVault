package com.mindvault.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
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
    private val active: ActiveDatabase,
) : DeckRepository {

    override fun observeAll(): Flow<List<Deck>> =
        active.observe { db -> db.deckDao().observeAll() }
            .map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Deck?> =
        active.observe { db -> db.deckDao().observeById(id) }
            .map { it?.toDomain() }

    override suspend fun getById(id: String): Deck? = active.current.deckDao().getById(id)?.toDomain()

    override suspend fun create(name: String): Deck {
        val db = active.current
        val now = nowMillis()
        val entity = DeckEntity(
            id = newId(),
            name = name.trim(),
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )
        return try {
            db.withTransaction {
                db.deckDao().insert(entity)
                db.syncOutboxDao().upsert(
                    outboxEntry(SyncEntityType.DECK, entity.id, SyncOperation.UPSERT, entity.updatedAt),
                )
            }
            entity.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(entity.name)
        }
    }

    override suspend fun rename(id: String, name: String): Deck? {
        val db = active.current
        val dao = db.deckDao()
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(name = name.trim(), updatedAt = nextUpdatedAt(existing.updatedAt))
        return try {
            db.withTransaction {
                dao.update(updated)
                db.syncOutboxDao().upsert(
                    outboxEntry(SyncEntityType.DECK, id, SyncOperation.UPSERT, updated.updatedAt),
                )
            }
            updated.toDomain()
        } catch (e: SQLiteConstraintException) {
            throw DuplicateNameException(updated.name)
        }
    }

    /** The deck's flashcards go with it (database cascade); only the deck's deletion is recorded for sync. */
    override suspend fun delete(id: String) {
        val db = active.current
        val dao = db.deckDao()
        val deck = dao.getById(id) ?: return
        if (deck.isDefault) throw DefaultDeckProtectedException()
        db.withTransaction {
            dao.deleteById(id)
            db.syncOutboxDao().upsert(
                outboxEntry(SyncEntityType.DECK, id, SyncOperation.DELETE, nowMillis()),
            )
        }
    }
}