package com.mindvault.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity

data class FlashcardWithDetails(
    @Embedded val flashcard: FlashcardEntity,
    @Relation(parentColumn = "knowledgeItemId", entityColumn = "id")
    val knowledgeItem: KnowledgeItemEntity,
    @Relation(parentColumn = "id", entityColumn = "flashcardId")
    val reviewState: ReviewStateEntity?,
)