package com.mindvault.data.sync

import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The conflict policy (Entry 17), one rule per test. */
class SyncApplierTest {
    private val store = FakeSyncLocalStore()
    private val applier = SyncApplier(store) { 1_000L }

    private fun apply(table: SyncTable, vararg rows: SyncRow) = runBlocking { rows.forEach { applier.apply(table, it) } }

    private fun pending(type: String, id: String) = store.outbox[outboxKey(type, id)]

    // ---- newer wins

    @Test
    fun newerRemoteItemReplacesLocalAndDropsItsPendingEdit() {
        store.addItem("i1", title = "Local", updated = 100)
        store.outbox[outboxKey(SyncEntityType.KNOWLEDGE_ITEM, "i1")] =
            outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, "i1", SyncOperation.UPSERT, 100)

        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i1", title = "Remote", created = 1, updated = 200))

        assertEquals("Remote", store.itemRows.getValue("i1").title)
        assertEquals(200L, store.itemRows.getValue("i1").updatedAt)
        assertNull(pending(SyncEntityType.KNOWLEDGE_ITEM, "i1"))
        assertEquals(0, applier.enqueued)
    }

    @Test
    fun olderOrEqualRemoteItemIsIgnored() {
        store.addItem("i1", title = "Local", updated = 100)
        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i1", title = "Older", updated = 50), itemRow("i1", title = "Same", updated = 100))
        assertEquals("Local", store.itemRows.getValue("i1").title)
    }

    @Test
    fun newRemoteItemIsInsertedWithoutAnOutboxEntry() {
        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i9", created = 5, updated = 7))
        assertEquals(7L, store.itemRows.getValue("i9").updatedAt)
        assertTrue(store.outbox.isEmpty())
    }

    // ---- deletion wins

    @Test
    fun remoteTombstoneDeletesItemItsCardsAndPendingChange() {
        store.addDeck("d1", "Deck")
        store.addItem("i1")
        store.addCard("c1", "i1", "d1")
        store.outbox[outboxKey(SyncEntityType.KNOWLEDGE_ITEM, "i1")] =
            outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, "i1", SyncOperation.UPSERT, 5)

        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i1", deleted = 300))

        assertNull(store.itemRows["i1"])
        assertTrue(store.cardRows.isEmpty())
        assertTrue(store.stateRows.isEmpty())
        assertTrue(store.outbox.isEmpty())
    }

    @Test
    fun itemDeletedHereIsNotBroughtBackByARemoteCopy() {
        store.outbox[outboxKey(SyncEntityType.KNOWLEDGE_ITEM, "i1")] =
            outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, "i1", SyncOperation.DELETE, 500)

        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i1", updated = 900))

        assertNull(store.itemRows["i1"])
        assertEquals(SyncOperation.DELETE, pending(SyncEntityType.KNOWLEDGE_ITEM, "i1")?.operation)
    }

    @Test
    fun defaultDeckTombstoneIsIgnored() {
        store.addDeck("default-deck", "Default", updated = 0, isDefault = true)
        apply(SyncTable.DECKS, deckRow("default-deck", "Default", deleted = 99))
        assertNotNull(store.deckRows["default-deck"])
    }

    @Test
    fun cardForAMissingItemOrDeckIsNotCreated() {
        store.addDeck("d1", "Deck")
        apply(SyncTable.FLASHCARDS, cardRow("c1", "missing-item", "d1"))
        assertTrue(store.cardRows.isEmpty())
    }

    // ---- same-name categories

    @Test
    fun olderRemoteCategoryIsKeptAndLocalItemsMoveToIt() {
        store.addCategory("local", "Science", created = 20)
        store.addItem("i1", categoryId = "local", updated = 100)

        apply(SyncTable.CATEGORIES, catRow("remote", "science", created = 10))

        assertEquals(setOf("remote"), store.categoryRows.keys)
        val moved = store.itemRows.getValue("i1")
        assertEquals("remote", moved.categoryId)
        assertTrue("updatedAt must move forward so the change wins in the cloud", moved.updatedAt > 100)
        assertEquals(SyncOperation.UPSERT, pending(SyncEntityType.KNOWLEDGE_ITEM, "i1")?.operation)
        assertEquals(SyncOperation.DELETE, pending(SyncEntityType.CATEGORY, "local")?.operation)
        assertEquals("remote", store.state[SyncStateKeys.categoryAlias("local")])
    }

    @Test
    fun olderLocalCategoryIsKeptAndTheRemoteOneIsRemovedFromTheCloud() {
        store.addCategory("local", "Science", created = 5)

        apply(SyncTable.CATEGORIES, catRow("remote", "Science", created = 10))

        assertEquals(setOf("local"), store.categoryRows.keys)
        assertEquals(SyncOperation.DELETE, pending(SyncEntityType.CATEGORY, "remote")?.operation)
        assertEquals("local", store.state[SyncStateKeys.categoryAlias("remote")])
    }

    @Test
    fun itemPulledWithALosingCategoryUsesTheKeptOneAndIsUploaded() {
        store.addCategory("local", "Science", created = 5)
        apply(SyncTable.CATEGORIES, catRow("remote", "Science", created = 10))

        apply(SyncTable.KNOWLEDGE_ITEMS, itemRow("i1", category = "remote", updated = 200))

        val item = store.itemRows.getValue("i1")
        assertEquals("local", item.categoryId)
        assertTrue(item.updatedAt > 200)
        assertEquals(SyncOperation.UPSERT, pending(SyncEntityType.KNOWLEDGE_ITEM, "i1")?.operation)
    }

    @Test
    fun remoteCategoryDeletionClearsTheCategoryOnLocalItems() {
        store.addCategory("c1", "Science")
        store.addItem("i1", categoryId = "c1", updated = 100)

        apply(SyncTable.CATEGORIES, catRow("c1", "Science", deleted = 500))

        assertNull(store.itemRows.getValue("i1").categoryId)
        assertEquals(100L, store.itemRows.getValue("i1").updatedAt) // the Vault order does not change
    }

    // ---- same-name decks

    @Test
    fun laterRemoteDeckGetsANumberedNameAndIsUploaded() {
        store.addDeck("local", "Spanish", created = 5)

        apply(SyncTable.DECKS, deckRow("remote", "spanish", created = 10, updated = 10))

        assertEquals("Spanish", store.deckRows.getValue("local").name)
        assertEquals("spanish (2)", store.deckRows.getValue("remote").name)
        assertEquals(SyncOperation.UPSERT, pending(SyncEntityType.DECK, "remote")?.operation)
        assertTrue(store.deckRows.getValue("remote").updatedAt > 10)
    }

    @Test
    fun laterLocalDeckIsRenamedWhenTheRemoteOneIsOlder() {
        store.addDeck("local", "Spanish", created = 20)

        apply(SyncTable.DECKS, deckRow("remote", "Spanish", created = 10))

        assertEquals("Spanish", store.deckRows.getValue("remote").name)
        assertEquals("Spanish (2)", store.deckRows.getValue("local").name)
        assertEquals(SyncOperation.UPSERT, pending(SyncEntityType.DECK, "local")?.operation)
    }

    @Test
    fun defaultDeckIsNeverRenamed() {
        store.addDeck("default-deck", "Default", created = 50, updated = 0, isDefault = true)

        apply(SyncTable.DECKS, deckRow("remote", "default", created = 1))

        assertEquals("Default", store.deckRows.getValue("default-deck").name)
        assertEquals("default (2)", store.deckRows.getValue("remote").name)
    }

    // ---- cards

    @Test
    fun ofTwoCardsForTheSameItemAndDeckTheEarlierIsKept_remoteEarlier() {
        store.addDeck("d1", "Deck")
        store.addItem("i1")
        store.addCard("local", "i1", "d1", created = 20)

        apply(SyncTable.FLASHCARDS, cardRow("remote", "i1", "d1", created = 10))

        assertEquals(setOf("remote"), store.cardRows.keys)
        assertEquals(SyncOperation.DELETE, pending(SyncEntityType.FLASHCARD, "local")?.operation)
        assertNotNull(store.stateRows["remote"])
    }

    @Test
    fun ofTwoCardsForTheSameItemAndDeckTheEarlierIsKept_localEarlier() {
        store.addDeck("d1", "Deck")
        store.addItem("i1")
        store.addCard("local", "i1", "d1", created = 5)

        apply(SyncTable.FLASHCARDS, cardRow("remote", "i1", "d1", created = 10))

        assertEquals(setOf("local"), store.cardRows.keys)
        assertEquals(SyncOperation.DELETE, pending(SyncEntityType.FLASHCARD, "remote")?.operation)
    }

    @Test
    fun pulledCardGetsAStartingReviewStateThatTheCloudStateOverrides() {
        store.addDeck("d1", "Deck")
        store.addItem("i1")

        apply(SyncTable.FLASHCARDS, cardRow("c1", "i1", "d1", created = 10))
        assertEquals(0L, store.stateRows.getValue("c1").updatedAt)

        apply(SyncTable.REVIEW_STATES, stateRow("c1", updated = 400, reviewCount = 3))
        assertEquals(3, store.stateRows.getValue("c1").reviewCount)
    }

    @Test
    fun olderRemoteReviewStateIsIgnored() {
        store.addDeck("d1", "Deck")
        store.addItem("i1")
        store.addCard("c1", "i1", "d1", created = 10)
        store.stateRows["c1"] = store.stateRows.getValue("c1").copy(reviewCount = 5, updatedAt = 500)

        apply(SyncTable.REVIEW_STATES, stateRow("c1", updated = 400, reviewCount = 1))

        assertEquals(5, store.stateRows.getValue("c1").reviewCount)
        assertFalse(store.outbox.isNotEmpty())
    }
}