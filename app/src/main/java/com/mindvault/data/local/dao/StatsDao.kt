package com.mindvault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.mindvault.data.local.relation.NameCountRow
import com.mindvault.data.local.relation.StatsCountsRow
import kotlinx.coroutines.flow.Flow

/** Read-only aggregates for Profile stats. Each query re-emits when any table it reads changes. */
@Dao
interface StatsDao {
    /**
     * Counts across the whole vault. Due, new and difficult use the same rules as the review queues:
     * due = reviewed and due before [startOfTomorrow] (calendar day); new = never reviewed;
     * difficult = reviewed with an ease factor below [difficultBelow]; learning / mature split on [matureDays].
     */
    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM knowledge_items) AS totalItems,
          (SELECT COUNT(*) FROM knowledge_items WHERE createdAt >= :weekStart) AS addedRecently,
          (SELECT COUNT(*) FROM knowledge_items WHERE categoryId IS NULL) AS uncategorizedItems,
          (SELECT COUNT(*) FROM categories) AS categoryCount,
          (SELECT COUNT(*) FROM decks) AS deckCount,
          (SELECT COUNT(*) FROM flashcards) AS flashcardCount,
          (SELECT COUNT(*) FROM review_states WHERE reviewCount = 0) AS newCards,
          (SELECT COUNT(*) FROM review_states WHERE reviewCount > 0 AND nextReviewAt < :startOfTomorrow) AS dueReviews,
          (SELECT COUNT(*) FROM review_states WHERE reviewCount > 0 AND easeFactor < :difficultBelow) AS difficult,
          (SELECT COUNT(*) FROM review_states WHERE reviewCount > 0 AND intervalDays < :matureDays) AS learning,
          (SELECT COUNT(*) FROM review_states WHERE reviewCount > 0 AND intervalDays >= :matureDays) AS mature
        """
    )
    fun observeCounts(
        weekStart: Long,
        startOfTomorrow: Long,
        difficultBelow: Double,
        matureDays: Int,
    ): Flow<StatsCountsRow>

    @Query("SELECT type AS name, COUNT(*) AS total FROM knowledge_items GROUP BY type")
    fun observeTypeCounts(): Flow<List<NameCountRow>>

    @Query(
        """
        SELECT c.name AS name, COUNT(k.id) AS total
        FROM categories c INNER JOIN knowledge_items k ON k.categoryId = c.id
        GROUP BY c.id
        ORDER BY total DESC, c.name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeTopCategories(limit: Int): Flow<List<NameCountRow>>

    @Query("SELECT rating AS name, COUNT(*) AS total FROM review_log GROUP BY rating")
    fun observeRatingCounts(): Flow<List<NameCountRow>>

    /** Review times since [since], newest first; the streak and the 7-day chart are computed from these. */
    @Query("SELECT reviewedAt FROM review_log WHERE reviewedAt >= :since ORDER BY reviewedAt DESC")
    fun observeReviewTimes(since: Long): Flow<List<Long>>
}