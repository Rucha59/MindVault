package com.mindvault.data.repository

import com.mindvault.core.util.newId
import com.mindvault.core.util.nowMillis
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import com.mindvault.data.mapper.toEntity
import com.mindvault.domain.model.ReviewState

/**
 * Category decks and card creation, shared by the repositories. Every function must be called inside a
 * transaction on [db]; each change records its outbox entries in that same transaction.
 */
internal object CategoryDecks {

    /** The deck made for [categoryId] (the oldest one, if several), or null. */
    suspend fun deckFor(db: MindVaultDatabase, categoryId: String): DeckEntity? =
        db.deckDao().findByCategory(categoryId)

    /**
     * The category's deck. If it has none, creates it (named after the category, "Name (2)" if that name is taken)
     * with a card for each item in the category.
     */
    suspend fun ensureDeck(db: MindVaultDatabase, category: CategoryEntity): DeckEntity {
        deckFor(db, category.id)?.let { return it }
        val now = nowMillis()
        val deck = DeckEntity(
            id = newId(),
            name = freeName(db, category.name, ignoreId = null),
            isDefault = false,
            createdAt = now,
            updatedAt = now,
            categoryId = category.id,
        )
        db.deckDao().insert(deck)
        db.syncOutboxDao().upsert(outboxEntry(SyncEntityType.DECK, deck.id, SyncOperation.UPSERT, deck.updatedAt))
        db.knowledgeItemDao().getByCategory(category.id).forEach { addCard(db, it.id, deck.id, now) }
        return deck
    }

    /** Adds the item to its category's deck if the category has one. A deck the user deleted is not recreated. */
    suspend fun addItem(db: MindVaultDatabase, itemId: String, categoryId: String, now: Long) {
        val deck = deckFor(db, categoryId) ?: return
        addCard(db, itemId, deck.id, now)
    }

    /** Removes the item's card (and its review progress) from the category's deck, if it has one there. */
    suspend fun removeItem(db: MindVaultDatabase, itemId: String, categoryId: String) {
        val deck = deckFor(db, categoryId) ?: return
        val card = db.flashcardDao().findByItemAndDeck(itemId, deck.id) ?: return
        db.flashcardDao().deleteById(card.id)
        db.syncOutboxDao().upsert(outboxEntry(SyncEntityType.FLASHCARD, card.id, SyncOperation.DELETE, nowMillis()))
    }

    /** Renames the category's deck to follow the category, unless the user gave the deck a different name. */
    suspend fun renameDeck(db: MindVaultDatabase, categoryId: String, oldName: String, newName: String) {
        val deck = deckFor(db, categoryId) ?: return
        if (!deck.name.equals(oldName, ignoreCase = true) || deck.name == newName) return
        val renamed = deck.copy(
            name = freeName(db, newName, ignoreId = deck.id),
            updatedAt = nextUpdatedAt(deck.updatedAt),
        )
        db.deckDao().update(renamed)
        db.syncOutboxDao().upsert(outboxEntry(SyncEntityType.DECK, deck.id, SyncOperation.UPSERT, renamed.updatedAt))
    }

    /** A new card (with its starting review state) for [itemId] in [deckId]. False if the item already has one there. */
    suspend fun addCard(db: MindVaultDatabase, itemId: String, deckId: String, now: Long): Boolean {
        val card = FlashcardEntity(id = newId(), knowledgeItemId = itemId, deckId = deckId, createdAt = now)
        if (db.flashcardDao().insert(card) == -1L) return false
        db.reviewStateDao().upsert(ReviewState.initial(card.id, now).toEntity(updatedAt = now))
        val outbox = db.syncOutboxDao()
        outbox.upsert(outboxEntry(SyncEntityType.FLASHCARD, card.id, SyncOperation.UPSERT, now))
        outbox.upsert(outboxEntry(SyncEntityType.REVIEW_STATE, card.id, SyncOperation.UPSERT, now))
        return true
    }

    /** [base] if no other deck uses it (names ignore case), else "base (2)", "base (3)"... */
    private suspend fun freeName(db: MindVaultDatabase, base: String, ignoreId: String?): String {
        val dao = db.deckDao()
        suspend fun free(name: String): Boolean = dao.findByName(name).let { it == null || it.id == ignoreId }
        if (free(base)) return base
        var n = 2
        while (!free("$base ($n)")) n++
        return "$base ($n)"
    }
}