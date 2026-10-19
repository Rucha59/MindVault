package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.relation.FlashcardWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    /** Returns -1 when the (knowledgeItemId, deckId) pair already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(flashcard: FlashcardEntity): Long

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun observeByDeck(deckId: String): Flow<List<FlashcardWithDetails>>

    @Transaction
    @Query(
        """
        SELECT f.* FROM flashcards f
        INNER JOIN review_states r ON r.flashcardId = f.id
        WHERE f.deckId = :deckId AND r.nextReviewAt <= :now
        ORDER BY r.nextReviewAt ASC
        """
    )
    fun observeDue(deckId: String, now: Long): Flow<List<FlashcardWithDetails>>

    @Query("SELECT deckId FROM flashcards WHERE knowledgeItemId = :knowledgeItemId")
    fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    fun observeCountByDeck(deckId: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM flashcards f
        INNER JOIN review_states r ON r.flashcardId = f.id
        WHERE f.deckId = :deckId AND r.reviewCount = 0
        """
    )
    fun observeUnreviewedCountByDeck(deckId: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM flashcards f
        INNER JOIN review_states r ON r.flashcardId = f.id
        WHERE f.deckId = :deckId AND r.nextReviewAt <= :now
        """
    )
    fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int>
}