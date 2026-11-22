package com.mindvault.data.sync

import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Category decks in the sync conflict policy. */
class SyncApplierDeckCategoryTest {
    private val store = FakeSyncLocalStore()
    private val applier = SyncApplier(store) { 1_000L }

    private fun apply(table: SyncTable, vararg rows: SyncRow) = runBlocking { rows.forEach { applier.apply(table, it) } }

    @Test
    fun aPulledDeckIsLinkedToItsCategory() {
        store.addCategory("c1", "Science")
        apply(SyncTable.DECKS, deckRow("d1", "Science", categoryId = "c1"))
        assertEquals("c1", store.deckRows.getValue("d1").categoryId)
        assertTrue(store.outbox.isEmpty())
    }

    @Test
    fun aDeckPointingAtAMergedAwayCategoryUsesTheKeptOneAndIsUploaded() {
        store.addCategory("local", "Science", created = 5)
        apply(SyncTable.CATEGORIES, catRow("remote", "Science", created = 10)) // the local one is older and is kept

        apply(SyncTable.DECKS, deckRow("d1", "Science cards", categoryId = "remote", updated = 200))

        val deck = store.deckRows.getValue("d1")
        assertEquals("local", deck.categoryId)
        assertTrue("updatedAt must move forward so the change wins in the cloud", deck.updatedAt > 200)
        assertEquals(SyncOperation.UPSERT, store.outbox[outboxKey(SyncEntityType.DECK, "d1")]?.operation)
    }

    @Test
    fun aDeckForAMissingCategoryIsStoredWithoutALink() {
        apply(SyncTable.DECKS, deckRow("d1", "Orphan", categoryId = "unknown"))
        assertNotNull(store.deckRows["d1"])
        assertNull(store.deckRows.getValue("d1").categoryId)
    }

    @Test
    fun aRemoteCategoryDeletionKeepsItsDeckAsAnOrdinaryDeck() {
        store.addCategory("c1", "Science")
        store.addDeck("d1", "Science", categoryId = "c1", updated = 100)

        apply(SyncTable.CATEGORIES, catRow("c1", "Science", deleted = 500))

        val deck = store.deckRows.getValue("d1")
        assertNull(deck.categoryId)
        assertEquals(100L, deck.updatedAt)
    }

    @Test
    fun aNewerRemoteDeckCanChangeItsCategoryLink() {
        store.addCategory("c1", "Science")
        store.addCategory("c2", "Physics")
        store.addDeck("d1", "Cards", categoryId = "c1", updated = 100)

        apply(SyncTable.DECKS, deckRow("d1", "Cards", categoryId = "c2", updated = 300))

        assertEquals("c2", store.deckRows.getValue("d1").categoryId)
    }
}