package com.mindvault.data.mapper

import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.relation.FlashcardWithDetails
import com.mindvault.domain.model.Flashcard
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewState

internal fun FlashcardEntity.toDomain() = Flashcard(
    id = id,
    knowledgeItemId = knowledgeItemId,
    deckId = deckId,
    createdAt = createdAt,
)

internal fun ReviewStateEntity.toDomain() = ReviewState(
    flashcardId = flashcardId,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    reviewCount = reviewCount,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
)

/** The domain ReviewState has no updatedAt; repositories pass the real save time when they write. */
internal fun ReviewState.toEntity(updatedAt: Long = lastReviewedAt ?: nextReviewAt) = ReviewStateEntity(
    flashcardId = flashcardId,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    reviewCount = reviewCount,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    updatedAt = updatedAt,
)

internal fun FlashcardWithDetails.toDomain() = FlashcardDetail(
    flashcard = flashcard.toDomain(),
    knowledgeItem = knowledgeItem.toDomain(),
    // Defensive: addToDeck always creates a state, but never crash on a missing row.
    reviewState = reviewState?.toDomain()
        ?: ReviewState.initial(flashcard.id, flashcard.createdAt),
)