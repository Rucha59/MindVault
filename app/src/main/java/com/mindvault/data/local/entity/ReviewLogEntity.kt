package com.mindvault.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One counted answer to a card (review history): Hard, Good or Easy. Again is not a category and is never logged; a
 * card rated Again is logged when it comes back and is answered. Extra practice is not logged. Append-only: rows are
 * inserted once and never changed. There is deliberately no foreign key to flashcards, so the history (and the
 * streak) survives deleting a card, deck or item. Added in schema v6.
 */
@Entity(tableName = "review_log", indices = [Index("reviewedAt")])
data class ReviewLogEntity(
    @PrimaryKey val id: String,
    val flashcardId: String,
    val rating: String,             // a ReviewRating name: HARD, GOOD or EASY (never AGAIN)
    val reviewedAt: Long,           // UTC epoch millis
    val intervalDaysBefore: Int,
    val intervalDaysAfter: Int,
)