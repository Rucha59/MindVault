package com.mindvault.data.sync

import com.mindvault.data.local.entity.SyncEntityType

/**
 * The `data` object sync_push expects for an UPSERT, read from the vault right now (not from the outbox, which
 * only says that something changed). Null when the row no longer exists. All timestamps are epoch milliseconds.
 * Field names must match docs/supabase_sync_schema.sql.
 */
internal suspend fun SyncLocalStore.upsertData(type: String, id: String): Map<String, Any?>? = when (type) {
    SyncEntityType.CATEGORY -> category(id)?.let {
        mapOf("name" to it.name, "created_at" to it.createdAt, "updated_at" to it.updatedAt)
    }
    SyncEntityType.DECK -> deck(id)?.let {
        mapOf(
            "name" to it.name,
            "is_default" to it.isDefault,
            "category_id" to it.categoryId,
            "created_at" to it.createdAt,
            "updated_at" to it.updatedAt,
        )
    }
    SyncEntityType.KNOWLEDGE_ITEM -> item(id)?.let {
        mapOf(
            "title" to it.title,
            "type" to it.type,
            "definition" to it.definition,
            "personal_notes" to it.personalNotes,
            "example" to it.example,
            "category_id" to it.categoryId,
            "created_at" to it.createdAt,
            "updated_at" to it.updatedAt,
        )
    }
    SyncEntityType.FLASHCARD -> flashcard(id)?.let {
        mapOf(
            "knowledge_item_id" to it.knowledgeItemId,
            "deck_id" to it.deckId,
            "created_at" to it.createdAt,
        )
    }
    SyncEntityType.REVIEW_STATE -> reviewState(id)?.let {
        mapOf(
            "last_reviewed_at" to it.lastReviewedAt,
            "next_review_at" to it.nextReviewAt,
            "review_count" to it.reviewCount,
            "ease_factor" to it.easeFactor,
            "interval_days" to it.intervalDays,
            "updated_at" to it.updatedAt,
        )
    }
    SyncEntityType.REVIEW_LOG -> reviewLog(id)?.let {
        mapOf(
            "flashcard_id" to it.flashcardId,
            "rating" to it.rating,
            "reviewed_at" to it.reviewedAt,
            "interval_days_before" to it.intervalDaysBefore,
            "interval_days_after" to it.intervalDaysAfter,
        )
    }
    else -> null
}