package com.mindvault.data.sync

import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.entity.SyncOutboxEntity

/**
 * In-memory vault that behaves like the Room database where it matters to sync: unique names (case-insensitive),
 * unique (item, deck) cards, foreign keys, SET NULL / CASCADE deletes and a protected default deck.
 * A rule the engine breaks throws, so a test fails instead of hiding a bug the real database would raise.
 */
class FakeSyncLocalStore : SyncLocalStore {
    val outbox = LinkedHashMap<String, SyncOutboxEntity>()
    val state = HashMap<String, String>()
    val categoryRows = LinkedHashMap<String, CategoryEntity>()
    val deckRows = LinkedHashMap<String, DeckEntity>()
    val itemRows = LinkedHashMap<String, KnowledgeItemEntity>()
    val cardRows = LinkedHashMap<String, FlashcardEntity>()
    val stateRows = LinkedHashMap<String, ReviewStateEntity>()

    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()

    override suspend fun outboxAll() = outbox.values.sortedWith(compareBy({ it.changedAt }, { it.entityType }, { it.entityId }))
    override suspend fun outboxGet(type: String, id: String) = outbox[outboxKey(type, id)]
    override suspend fun outboxPut(entry: SyncOutboxEntity) { outbox[outboxKey(entry.entityType, entry.entityId)] = entry }
    override suspend fun outboxPutIfAbsent(entry: SyncOutboxEntity) {
        outbox.putIfAbsent(outboxKey(entry.entityType, entry.entityId), entry)
    }
    override suspend fun outboxRemove(type: String, id: String) { outbox.remove(outboxKey(type, id)) }
    override suspend fun outboxRemoveIfUnchanged(entry: SyncOutboxEntity) {
        val key = outboxKey(entry.entityType, entry.entityId)
        if (outbox[key] == entry) outbox.remove(key)
    }

    override suspend fun stateGet(key: String) = state[key]
    override suspend fun statePut(key: String, value: String) { state[key] = value }
    override suspend fun stateRemove(key: String) { state.remove(key) }

    // Categories
    override suspend fun categories() = categoryRows.values.toList()
    override suspend fun category(id: String) = categoryRows[id]
    override suspend fun categoryByName(name: String) = categoryRows.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
    override suspend fun insertCategory(category: CategoryEntity) {
        check(category.id !in categoryRows) { "duplicate category id" }
        checkCategoryName(category)
        categoryRows[category.id] = category
    }
    override suspend fun updateCategory(category: CategoryEntity) {
        check(category.id in categoryRows) { "no such category" }
        checkCategoryName(category)
        categoryRows[category.id] = category
    }
    private fun checkCategoryName(c: CategoryEntity) =
        check(categoryRows.values.none { it.id != c.id && it.name.equals(c.name, ignoreCase = true) }) { "UNIQUE categories.name: ${c.name}" }
    override suspend fun deleteCategory(id: String) {
        if (categoryRows.remove(id) == null) return
        itemRows.replaceAll { _, item -> if (item.categoryId == id) item.copy(categoryId = null) else item }
        deckRows.replaceAll { _, deck -> if (deck.categoryId == id) deck.copy(categoryId = null) else deck } // SET NULL
    }

    // Decks
    override suspend fun decks() = deckRows.values.toList()
    override suspend fun deck(id: String) = deckRows[id]
    override suspend fun deckByName(name: String) = deckRows.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
    override suspend fun insertDeck(deck: DeckEntity) {
        check(deck.id !in deckRows) { "duplicate deck id" }
        checkDeckName(deck)
        checkDeckCategory(deck)
        deckRows[deck.id] = deck
    }
    override suspend fun updateDeck(deck: DeckEntity) {
        check(deck.id in deckRows) { "no such deck" }
        checkDeckName(deck)
        checkDeckCategory(deck)
        deckRows[deck.id] = deck
    }
    private fun checkDeckCategory(d: DeckEntity) =
        check(d.categoryId == null || d.categoryId in categoryRows) { "FOREIGN KEY: deck category ${d.categoryId}" }
    private fun checkDeckName(d: DeckEntity) =
        check(deckRows.values.none { it.id != d.id && it.name.equals(d.name, ignoreCase = true) }) { "UNIQUE decks.name: ${d.name}" }
    override suspend fun deleteDeck(id: String) {
        if (deckRows[id]?.isDefault == true) return // like DeckDao: the default deck is never deleted
        if (deckRows.remove(id) == null) return
        cardRows.values.filter { it.deckId == id }.forEach { removeCard(it.id) }
    }

    // Items
    override suspend fun items() = itemRows.values.toList()
    override suspend fun item(id: String) = itemRows[id]
    override suspend fun itemsInCategory(categoryId: String) = itemRows.values.filter { it.categoryId == categoryId }
    override suspend fun insertItem(item: KnowledgeItemEntity) {
        check(item.id !in itemRows) { "duplicate item id" }
        checkCategoryExists(item)
        itemRows[item.id] = item
    }
    override suspend fun updateItem(item: KnowledgeItemEntity) {
        check(item.id in itemRows) { "no such item" }
        checkCategoryExists(item)
        itemRows[item.id] = item
    }
    private fun checkCategoryExists(item: KnowledgeItemEntity) =
        check(item.categoryId == null || item.categoryId in categoryRows) { "FOREIGN KEY: category ${item.categoryId}" }
    override suspend fun deleteItem(id: String) {
        if (itemRows.remove(id) == null) return
        cardRows.values.filter { it.knowledgeItemId == id }.forEach { removeCard(it.id) }
    }

    // Flashcards
    override suspend fun flashcards() = cardRows.values.toList()
    override suspend fun flashcard(id: String) = cardRows[id]
    override suspend fun flashcardFor(knowledgeItemId: String, deckId: String) =
        cardRows.values.firstOrNull { it.knowledgeItemId == knowledgeItemId && it.deckId == deckId }
    override suspend fun insertFlashcard(flashcard: FlashcardEntity): Boolean {
        check(flashcard.knowledgeItemId in itemRows && flashcard.deckId in deckRows) { "FOREIGN KEY: flashcard parents" }
        if (flashcard.id in cardRows || flashcardFor(flashcard.knowledgeItemId, flashcard.deckId) != null) return false
        cardRows[flashcard.id] = flashcard
        return true
    }
    override suspend fun deleteFlashcard(id: String) = removeCard(id)
    private fun removeCard(id: String) {
        cardRows.remove(id)
        stateRows.remove(id)
    }

    // Review states
    override suspend fun reviewStates() = stateRows.values.toList()
    override suspend fun reviewState(flashcardId: String) = stateRows[flashcardId]
    override suspend fun putReviewState(state: ReviewStateEntity) {
        check(state.flashcardId in cardRows) { "FOREIGN KEY: review state card" }
        stateRows[state.flashcardId] = state
    }
    override fun initialReviewState(flashcardId: String, createdAt: Long) =
        ReviewStateEntity(flashcardId, null, createdAt, 0, 2.5, 0, 0L)

    // Test helpers: add rows the way the app would have, without going through the outbox.
    fun addCategory(id: String, name: String, created: Long = 1L, updated: Long = created) =
        CategoryEntity(id, name, created, updated).also { categoryRows[id] = it }
    fun addDeck(
        id: String, name: String, created: Long = 1L, updated: Long = created,
        isDefault: Boolean = false, categoryId: String? = null,
    ) = DeckEntity(id, name, isDefault, created, updated, categoryId).also { deckRows[id] = it }    fun addItem(id: String, title: String = "Title $id", categoryId: String? = null, created: Long = 1L, updated: Long = created) =
        KnowledgeItemEntity(id, title, "WORD", "Definition", "", null, categoryId, created, updated).also { itemRows[id] = it }
    fun addCard(id: String, itemId: String, deckId: String, created: Long = 1L): FlashcardEntity {
        val card = FlashcardEntity(id, itemId, deckId, created)
        cardRows[id] = card
        stateRows[id] = initialReviewState(id, created).copy(updatedAt = created)
        return card
    }
}