package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.data.local.entity.ReviewLogEntity

@Dao
interface ReviewLogDao {
    /** Returns -1 when an entry with this ID already exists (append-only: it is never replaced). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: ReviewLogEntity): Long

    @Query("SELECT * FROM review_log WHERE id = :id")
    suspend fun getById(id: String): ReviewLogEntity?
}