package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mindvault.data.local.entity.KnowledgeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeItemDao {
    @Insert
    suspend fun insert(item: KnowledgeItemEntity)

    @Update
    suspend fun update(item: KnowledgeItemEntity): Int

    @Query("DELETE FROM knowledge_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM knowledge_items WHERE id = :id")
    suspend fun getById(id: String): KnowledgeItemEntity?

    @Query("SELECT * FROM knowledge_items WHERE id = :id")
    fun observeById(id: String): Flow<KnowledgeItemEntity?>

    @Query("SELECT * FROM knowledge_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_items WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByTitle(title: String): KnowledgeItemEntity?
}