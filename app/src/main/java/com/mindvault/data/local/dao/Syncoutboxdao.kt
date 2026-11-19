package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {
    /** One pending entry per entity: a newer change replaces the older one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SyncOutboxEntity)

    /** Used by the first full upload: never replaces a pending change that is already there. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entry: SyncOutboxEntity): Long

    @Query("SELECT * FROM sync_outbox ORDER BY changedAt, entityType, entityId")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE entityType = :type AND entityId = :id")
    suspend fun get(type: String, id: String): SyncOutboxEntity?

    @Query("DELETE FROM sync_outbox WHERE entityType = :type AND entityId = :id")
    suspend fun delete(type: String, id: String)

    /** Removes an entry only if it was not replaced by a newer change while it was being uploaded. */
    @Query(
        "DELETE FROM sync_outbox WHERE entityType = :type AND entityId = :id " +
                "AND operation = :operation AND changedAt = :changedAt"
    )
    suspend fun deleteIfUnchanged(type: String, id: String, operation: String, changedAt: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observeCount(): Flow<Int>
}