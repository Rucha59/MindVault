package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindvault.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReviewStateEntity)

    @Query("SELECT * FROM review_states WHERE flashcardId = :flashcardId")
    suspend fun getByFlashcardId(flashcardId: String): ReviewStateEntity?

    @Query("SELECT * FROM review_states WHERE flashcardId = :flashcardId")
    fun observeByFlashcardId(flashcardId: String): Flow<ReviewStateEntity?>
}