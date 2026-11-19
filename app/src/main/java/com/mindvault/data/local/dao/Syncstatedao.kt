package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: SyncStateEntity)

    @Query("SELECT stateValue FROM sync_state WHERE stateKey = :key")
    suspend fun get(key: String): String?

    @Query("SELECT stateValue FROM sync_state WHERE stateKey = :key")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM sync_state WHERE stateKey = :key")
    suspend fun delete(key: String)
}