package com.mindvault.data.local.entity

import androidx.room.Entity

/**
 * A change that still has to be sent to the cloud. One row per entity: a newer change replaces an older
 * one (for example a delete replaces a pending edit). Written by the repositories in the same transaction
 * as the change itself, so a crash can never lose a pending change.
 */
@Entity(tableName = "sync_outbox", primaryKeys = ["entityType", "entityId"])
data class SyncOutboxEntity(
    val entityType: String, // a SyncEntityType value
    val entityId: String,   // the entity's ID (for review states: the flashcard ID)
    val operation: String,  // a SyncOperation value
    val changedAt: Long,    // UTC epoch millis of the change; becomes the deletion time for DELETE
)

object SyncEntityType {
    const val CATEGORY = "CATEGORY"
    const val DECK = "DECK"
    const val KNOWLEDGE_ITEM = "KNOWLEDGE_ITEM"
    const val FLASHCARD = "FLASHCARD"
    const val REVIEW_STATE = "REVIEW_STATE"
}

object SyncOperation {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

fun outboxEntry(entityType: String, entityId: String, operation: String, changedAt: Long) =
    SyncOutboxEntity(entityType, entityId, operation, changedAt)