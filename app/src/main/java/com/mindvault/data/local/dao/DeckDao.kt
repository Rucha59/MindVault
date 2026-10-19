package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mindvault.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: DeckEntity)

    @Update
    suspend fun update(deck: DeckEntity): Int

    // The default deck can never be deleted, even if a caller forgets to check.
    @Query("DELETE FROM decks WHERE id = :id AND isDefault = 0")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeById(id: String): Flow<DeckEntity?>

    @Query("SELECT * FROM decks ORDER BY isDefault DESC, name")
    fun observeAll(): Flow<List<DeckEntity>>
}