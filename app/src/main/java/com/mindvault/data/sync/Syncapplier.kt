package com.mindvault.data.sync

import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry

/**
 * Applies rows pulled from the cloud to the vault, following the confirmed conflict policy
 * (docs/CODE_CHANGES.md, Entry 17):
 *  - the newer updatedAt wins for a whole row; an equal or older remote row is ignored;
 *  - a deletion wins over a concurrent edit, and a card for a deleted item or deck is not created;
 *  - same-name categories merge (older kept, items re-pointed); the later deck becomes "Name (2)";
 *    for two cards of the same item and deck the earlier one is kept.
 * Pulled rows are written without outbox entries, so they are never uploaded again. Where the app itself changes
 * something while resolving a conflict (merge, rename, duplicate removal), it queues that change for upload.
 * Call inside a transaction.
 */
internal class SyncApplier(
    private val store: SyncLocalStore,
    private val now: () -> Long,
) {
    /** Outbox entries created while resolving conflicts. The engine uploads again when this is above 0. */
    var enqueued: Int = 0
        private set

    suspend fun apply(table: SyncTable, row: SyncRow) {
        when (table) {
            SyncTable.CATEGORIES -> category(row)
            SyncTable.DECKS -> deck(row)
            SyncTable.KNOWLEDGE_ITEMS -> item(row)
            SyncTable.FLASHCARDS -> flashcard(row)
            SyncTable.REVIEW_STATES -> reviewState(row)
        }
    }

    // ---------------------------------------------------------------- categories

    private suspend fun category(row: SyncRow) {
        val id = row.str("id") ?: return
        if (row.long("deleted_at") != null) {
            store.deleteCategory(id) // items keep existing and lose the category (database rule)
            store.outboxRemove(SyncEntityType.CATEGORY, id)
            return
        }
        val name = row.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val createdAt = row.long("created_at") ?: return
        val updatedAt = row.long("updated_at") ?: return

        val local = store.category(id)
        if (local == null) {
            if (pendingDelete(SyncEntityType.CATEGORY, id)) return
            if (store.stateGet(SyncStateKeys.categoryAlias(id)) != null) {
                // This category lost an earlier same-name merge; make sure the cloud copy gets removed.
                if (store.outboxGet(SyncEntityType.CATEGORY, id) == null) {
                    enqueue(SyncEntityType.CATEGORY, id, SyncOperation.DELETE)
                }
                return
            }
        } else if (updatedAt <= local.updatedAt) {
            return
        }

        val incoming = CategoryEntity(id, name, local?.createdAt ?: createdAt, updatedAt)
        val clash = store.categoryByName(name)?.takeIf { it.id != id }
        if (clash == null) {
            if (local == null) {
                store.insertCategory(incoming)
            } else {
                store.updateCategory(incoming)
                dropPendingUpsert(SyncEntityType.CATEGORY, id)
            }
            return
        }
        mergeCategories(incoming, existsLocally = local != null, clash = clash)
    }

    /** [incoming] and [clash] have the same name. The older one is kept and the other one's items move to it. */
    private suspend fun mergeCategories(incoming: CategoryEntity, existsLocally: Boolean, clash: CategoryEntity) {
        if (isOlder(incoming.createdAt, incoming.id, clash.createdAt, clash.id)) {
            // The incoming category is kept. Free the name, put it in place, then retire the local one.
            store.updateCategory(clash.copy(name = TEMP_NAME_PREFIX + clash.id))
            if (existsLocally) {
                store.updateCategory(incoming)
                dropPendingUpsert(SyncEntityType.CATEGORY, incoming.id)
            } else {
                store.insertCategory(incoming)
            }
            moveItems(from = clash.id, to = incoming.id)
            store.deleteCategory(clash.id)
            setAlias(clash.id, incoming.id)
            enqueue(SyncEntityType.CATEGORY, clash.id, SyncOperation.DELETE) // replaces any pending edit of it
        } else {
            // The local category is kept. The incoming one is retired (and removed from the cloud).
            if (existsLocally) {
                moveItems(from = incoming.id, to = clash.id)
                store.deleteCategory(incoming.id)
            }
            setAlias(incoming.id, clash.id)
            enqueue(SyncEntityType.CATEGORY, incoming.id, SyncOperation.DELETE)
        }
    }

    /** Re-points items. updatedAt must move forward or the change would lose against the cloud copy. */
    private suspend fun moveItems(from: String, to: String) {
        for (item in store.itemsInCategory(from)) {
            val moved = item.copy(categoryId = to, updatedAt = bump(item.updatedAt))
            store.updateItem(moved)
            enqueue(SyncEntityType.KNOWLEDGE_ITEM, moved.id, SyncOperation.UPSERT, moved.updatedAt)
        }
    }

    // ---------------------------------------------------------------- decks

    private suspend fun deck(row: SyncRow) {
        val id = row.str("id") ?: return
        if (row.long("deleted_at") != null) {
            val local = store.deck(id)
            if (local != null && !local.isDefault) { // the default deck can never be deleted
                store.deleteDeck(id)
                store.outboxRemove(SyncEntityType.DECK, id)
            }
            return
        }
        val name = row.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val createdAt = row.long("created_at") ?: return
        val updatedAt = row.long("updated_at") ?: return

        val local = store.deck(id)
        if (local == null) {
            if (pendingDelete(SyncEntityType.DECK, id)) return
        } else if (updatedAt <= local.updatedAt) {
            return
        }

        val incoming = DeckEntity(
            id = id,
            name = name,
            isDefault = local?.isDefault ?: (row.bool("is_default") ?: false),
            createdAt = local?.createdAt ?: createdAt,
            updatedAt = updatedAt,
        )
        val clash = store.deckByName(name)?.takeIf { it.id != id }
        if (clash == null) {
            writeDeck(incoming, local != null)
            return
        }

        // Same name on two decks: the later one is renamed. The default deck is never renamed.
        val renameIncoming = when {
            incoming.isDefault -> false
            clash.isDefault -> true
            else -> !isOlder(incoming.createdAt, incoming.id, clash.createdAt, clash.id)
        }
        if (renameIncoming) {
            val renamed = incoming.copy(name = freeDeckName(name), updatedAt = bump(updatedAt))
            writeDeck(renamed, local != null)
            enqueue(SyncEntityType.DECK, id, SyncOperation.UPSERT, renamed.updatedAt)
        } else {
            val renamed = clash.copy(name = freeDeckName(clash.name), updatedAt = bump(clash.updatedAt))
            store.updateDeck(renamed)
            enqueue(SyncEntityType.DECK, clash.id, SyncOperation.UPSERT, renamed.updatedAt)
            writeDeck(incoming, local != null)
        }
    }

    private suspend fun writeDeck(deck: DeckEntity, exists: Boolean) {
        if (exists) {
            store.updateDeck(deck)
            dropPendingUpsert(SyncEntityType.DECK, deck.id)
        } else {
            store.insertDeck(deck)
        }
    }

    private suspend fun freeDeckName(base: String): String {
        var n = 2
        while (true) {
            val candidate = "$base ($n)"
            if (store.deckByName(candidate) == null) return candidate
            n++
        }
    }

    // ---------------------------------------------------------------- knowledge items

    private suspend fun item(row: SyncRow) {
        val id = row.str("id") ?: return
        if (row.long("deleted_at") != null) {
            store.deleteItem(id) // cards and review states go with it (database cascade)
            store.outboxRemove(SyncEntityType.KNOWLEDGE_ITEM, id)
            return
        }
        val title = row.str("title")?.takeIf { it.isNotBlank() } ?: return
        val definition = row.str("definition") ?: return
        val createdAt = row.long("created_at") ?: return
        val updatedAt = row.long("updated_at") ?: return

        val local = store.item(id)
        if (local == null) {
            if (pendingDelete(SyncEntityType.KNOWLEDGE_ITEM, id)) return
        } else if (updatedAt <= local.updatedAt) {
            return
        }

        val rawCategory = row.str("category_id")
        val category = resolveCategory(rawCategory)
        // The item pointed at a category that lost a merge: keep the surviving one and tell the cloud.
        val redirected = rawCategory != null && category != null && category != rawCategory
        val finalUpdatedAt = if (redirected) bump(updatedAt) else updatedAt
        val entity = KnowledgeItemEntity(
            id = id,
            title = title,
            type = row.str("type") ?: "OTHER",
            definition = definition,
            personalNotes = row.str("personal_notes") ?: "",
            example = row.str("example"),
            categoryId = category,
            createdAt = local?.createdAt ?: createdAt,
            updatedAt = finalUpdatedAt,
        )
        if (local == null) {
            store.insertItem(entity)
        } else {
            store.updateItem(entity)
            dropPendingUpsert(SyncEntityType.KNOWLEDGE_ITEM, id)
        }
        if (redirected) enqueue(SyncEntityType.KNOWLEDGE_ITEM, id, SyncOperation.UPSERT, finalUpdatedAt)
    }

    /** The category to use locally: the one named, or the one it was merged into, or none. */
    private suspend fun resolveCategory(raw: String?): String? {
        var current = raw ?: return null
        repeat(MAX_ALIAS_DEPTH) {
            if (store.category(current) != null) return current
            current = store.stateGet(SyncStateKeys.categoryAlias(current)) ?: return null
        }
        return null
    }

    // ---------------------------------------------------------------- flashcards and review states

    private suspend fun flashcard(row: SyncRow) {
        val id = row.str("id") ?: return
        if (row.long("deleted_at") != null) {
            store.deleteFlashcard(id)
            store.outboxRemove(SyncEntityType.FLASHCARD, id)
            store.outboxRemove(SyncEntityType.REVIEW_STATE, id)
            return
        }
        val itemId = row.str("knowledge_item_id") ?: return
        val deckId = row.str("deck_id") ?: return
        val createdAt = row.long("created_at") ?: return

        if (store.flashcard(id) != null) return // cards never change once created
        if (pendingDelete(SyncEntityType.FLASHCARD, id)) return
        // Deletion wins: no card for an item or deck that is gone here.
        if (store.item(itemId) == null || store.deck(deckId) == null) return

        val duplicate = store.flashcardFor(itemId, deckId)
        if (duplicate != null) {
            if (!isOlder(createdAt, id, duplicate.createdAt, duplicate.id)) {
                // The local card is the earlier one: keep it, remove the incoming card from the cloud.
                enqueue(SyncEntityType.FLASHCARD, id, SyncOperation.DELETE)
                return
            }
            store.deleteFlashcard(duplicate.id)
            store.outboxRemove(SyncEntityType.REVIEW_STATE, duplicate.id)
            enqueue(SyncEntityType.FLASHCARD, duplicate.id, SyncOperation.DELETE) // replaces a pending add
        }
        store.insertFlashcard(FlashcardEntity(id = id, knowledgeItemId = itemId, deckId = deckId, createdAt = createdAt))
        // Every card needs a review state; the cloud's own state (pulled next) overrides this one.
        store.putReviewState(store.initialReviewState(id, createdAt))
    }

    private suspend fun reviewState(row: SyncRow) {
        val id = row.str("id") ?: return // the flashcard's ID
        if (store.flashcard(id) == null) return
        val nextReviewAt = row.long("next_review_at") ?: return
        val reviewCount = row.int("review_count") ?: return
        val easeFactor = row.double("ease_factor") ?: return
        val intervalDays = row.int("interval_days") ?: return
        val updatedAt = row.long("updated_at") ?: return

        val local = store.reviewState(id)
        if (local != null && updatedAt <= local.updatedAt) return
        store.putReviewState(
            ReviewStateEntity(
                flashcardId = id,
                lastReviewedAt = row.long("last_reviewed_at"),
                nextReviewAt = nextReviewAt,
                reviewCount = reviewCount,
                easeFactor = easeFactor,
                intervalDays = intervalDays,
                updatedAt = updatedAt,
            ),
        )
        if (local != null) dropPendingUpsert(SyncEntityType.REVIEW_STATE, id)
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun pendingDelete(type: String, id: String): Boolean =
        store.outboxGet(type, id)?.operation == SyncOperation.DELETE

    /** The cloud version won, so a pending local edit of the same row must not be uploaded. */
    private suspend fun dropPendingUpsert(type: String, id: String) {
        if (store.outboxGet(type, id)?.operation == SyncOperation.UPSERT) store.outboxRemove(type, id)
    }

    private suspend fun enqueue(type: String, id: String, operation: String, changedAt: Long = now()) {
        store.outboxPut(outboxEntry(type, id, operation, changedAt))
        enqueued++
    }

    private suspend fun setAlias(loser: String, keeper: String) =
        store.statePut(SyncStateKeys.categoryAlias(loser), keeper)

    /** Never earlier than the previous version plus 1 ms, like the repositories' nextUpdatedAt. */
    private fun bump(previous: Long): Long = maxOf(now(), previous + 1)

    /** Older by creation time; equal times fall back to the smaller ID so every device picks the same one. */
    private fun isOlder(createdA: Long, idA: String, createdB: Long, idB: String): Boolean =
        createdA < createdB || (createdA == createdB && idA < idB)

    private companion object {
        const val TEMP_NAME_PREFIX = "~merging~"
        const val MAX_ALIAS_DEPTH = 5
    }
}