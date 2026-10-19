package com.mindvault.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledgeItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // One card per item per deck. Also serves lookups by knowledgeItemId.
        Index(value = ["knowledgeItemId", "deckId"], unique = true),
        Index("deckId"),
    ],
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val knowledgeItemId: String,
    val deckId: String,
    val createdAt: Long, // UTC epoch millis
)