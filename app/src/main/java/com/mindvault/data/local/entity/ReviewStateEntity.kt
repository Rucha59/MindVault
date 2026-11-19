package com.mindvault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nextReviewAt")],
)
data class ReviewStateEntity(
    @PrimaryKey val flashcardId: String,
    val lastReviewedAt: Long?, // UTC epoch millis, null until first review
    val nextReviewAt: Long,    // UTC epoch millis
    val reviewCount: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    /** Last time this state was saved, UTC epoch millis. Decides sync conflicts. Added in schema v2. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
)