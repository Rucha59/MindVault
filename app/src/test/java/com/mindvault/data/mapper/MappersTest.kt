package com.mindvault.data.mapper

import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.relation.FlashcardWithDetails
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun knowledgeItem_roundTripsThroughEntity() {
        val item = KnowledgeItem(
            id = "k1", title = "Ephemeral", type = KnowledgeType.WORD,
            definition = "Lasting a very short time", personalNotes = "Like morning dew",
            example = "Fame is ephemeral.", categoryId = "c1", createdAt = 10L, updatedAt = 20L,
        )
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun unknownStoredType_fallsBackToOther() {
        assertEquals(KnowledgeType.OTHER, "SYMBOL".toKnowledgeType())
    }

    @Test
    fun flashcardWithoutReviewState_getsInitialState() {
        val details = FlashcardWithDetails(
            flashcard = FlashcardEntity("f1", "k1", "d1", createdAt = 5L),
            knowledgeItem = KnowledgeItemEntity("k1", "T", "WORD", "D", "", null, null, 1L, 1L),
            reviewState = null,
        )
        assertEquals(ReviewState.initial("f1", 5L), details.toDomain().reviewState)
    }
}