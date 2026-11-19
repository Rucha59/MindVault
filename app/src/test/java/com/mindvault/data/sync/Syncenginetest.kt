package com.mindvault.data.sync

import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {
    private val store = FakeSyncLocalStore()
    private val remote = FakeSyncRemote()
    private val engine = SyncEngine(remote, now = { 5_000L }, pageSize = 2)

    private fun run() = runBlocking { engine.run(store, "user-1") }

    private fun queue(type: String, id: String, op: String = SyncOperation.UPSERT, at: Long = 1L) {
        store.outbox[outboxKey(type, id)] = outboxEntry(type, id, op, at)
    }

    private fun tree() {
        store.addCategory("cat", "Science")
        store.addDeck("deck", "Deck")
        store.addItem("item", categoryId = "cat")
        store.addCard("card", "item", "deck")
    }

    // ---- push

    @Test
    fun firstRunUploadsEveryExistingRowParentsFirst() {
        tree()
        val result = run() as SyncRunResult.Success

        val order = remote.allPushed.map { it.type }
        fun first(type: String) = order.indexOf(type)
        assertTrue(first(SyncEntityType.CATEGORY) < first(SyncEntityType.KNOWLEDGE_ITEM))
        assertTrue(first(SyncEntityType.DECK) < first(SyncEntityType.FLASHCARD))
        assertTrue(first(SyncEntityType.KNOWLEDGE_ITEM) < first(SyncEntityType.FLASHCARD))
        assertTrue(first(SyncEntityType.FLASHCARD) < first(SyncEntityType.REVIEW_STATE))
        assertEquals(5, result.uploaded)
        assertEquals(0, result.remaining)
        assertTrue(store.outbox.isEmpty())
    }

    @Test
    fun firstFullUploadIsQueuedOnlyOnce() {
        tree()
        run()
        remote.pushes.clear()
        store.addItem("later") // added without an outbox entry, like a row from before sync existed

        run()
        assertTrue("the first-upload flag is already set", remote.allPushed.isEmpty())
    }

    @Test
    fun deletionsGoLastAndChildrenFirst() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        queue(SyncEntityType.CATEGORY, "cat", SyncOperation.DELETE)
        queue(SyncEntityType.DECK, "deck", SyncOperation.DELETE)
        queue(SyncEntityType.KNOWLEDGE_ITEM, "item", SyncOperation.DELETE)
        queue(SyncEntityType.FLASHCARD, "card", SyncOperation.DELETE)
        store.addCategory("other", "Other")
        queue(SyncEntityType.CATEGORY, "other")

        run()

        assertEquals(
            listOf(
                SyncEntityType.CATEGORY, // the upsert comes before every deletion
                SyncEntityType.FLASHCARD, SyncEntityType.KNOWLEDGE_ITEM, SyncEntityType.DECK, SyncEntityType.CATEGORY,
            ),
            remote.allPushed.map { it.type },
        )
    }

    @Test
    fun upsertForARowThatIsGoneIsDroppedWithoutBeingSent() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        queue(SyncEntityType.KNOWLEDGE_ITEM, "gone")

        val result = run() as SyncRunResult.Success

        assertTrue(remote.allPushed.isEmpty())
        assertEquals(0, result.remaining)
    }

    @Test
    fun rejectedChangeStaysQueuedAndTheRestIsRemoved() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        store.addCategory("a", "A")
        store.addCategory("b", "B")
        queue(SyncEntityType.CATEGORY, "a")
        queue(SyncEntityType.CATEGORY, "b")
        remote.rejected = setOf(outboxKey(SyncEntityType.CATEGORY, "a"))

        val result = run() as SyncRunResult.Success

        assertEquals(1, result.rejected)
        assertEquals(1, result.remaining)
        assertNotNull(store.outbox[outboxKey(SyncEntityType.CATEGORY, "a")])
        assertNull(store.outbox[outboxKey(SyncEntityType.CATEGORY, "b")])
    }

    @Test
    fun cardWaitsWhileItsItemCouldNotBeUploaded() {
        tree()
        remote.rejected = setOf(outboxKey(SyncEntityType.KNOWLEDGE_ITEM, "item"))

        run()

        val sent = remote.allPushed.map { it.type }
        assertTrue(SyncEntityType.FLASHCARD !in sent)
        assertTrue(SyncEntityType.REVIEW_STATE !in sent)
        assertNotNull("the card must not be lost", store.outbox[outboxKey(SyncEntityType.FLASHCARD, "card")])
    }

    @Test
    fun changeMadeWhileUploadingIsKeptAndSentAgain() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        store.addItem("item", updated = 10)
        queue(SyncEntityType.KNOWLEDGE_ITEM, "item", at = 10)
        remote.onPush = { queue(SyncEntityType.KNOWLEDGE_ITEM, "item", at = 20) } // an edit during the upload

        val result = run() as SyncRunResult.Success

        assertEquals(2, remote.allPushed.count { it.type == SyncEntityType.KNOWLEDGE_ITEM })
        assertEquals(0, result.remaining)
    }

    @Test
    fun networkFailureKeepsEverythingQueued() {
        tree()
        remote.failure = SyncFailureKind.NETWORK

        val result = run()

        assertEquals(SyncRunResult.Failure(SyncFailureKind.NETWORK, 5), result)
        assertEquals(5, store.outbox.size)
    }

    // ---- pull

    @Test
    fun pullStartsFromTheSavedCursorMinusFiveMinutes() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        store.state[SyncStateKeys.cursor(SyncTable.CATEGORIES)] = "2026-01-01T10:00:00Z"

        run()

        val call = remote.pullCalls.first { it.table == SyncTable.CATEGORIES }
        assertEquals("2026-01-01T09:55:00Z", call.since)
        assertEquals("", call.afterId)
    }

    @Test
    fun firstPullStartsAtTheEpoch() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        run()
        assertEquals("1970-01-01T00:00:00Z", remote.pullCalls.first().since)
    }

    @Test
    fun pullReadsEveryPageAndSavesTheNewestCursor() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        remote.rows.getValue(SyncTable.CATEGORIES) += listOf(
            catRow("c1", "One", server = "2026-01-01T00:00:01Z"),
            catRow("c2", "Two", server = "2026-01-01T00:00:02Z"),
            catRow("c3", "Three", server = "2026-01-01T00:00:03Z"),
        )

        val result = run() as SyncRunResult.Success

        assertEquals(3, store.categoryRows.size)
        assertEquals(3, result.downloaded)
        assertEquals("2026-01-01T00:00:03Z", store.state[SyncStateKeys.cursor(SyncTable.CATEGORIES)])
        assertEquals(2, remote.pullCalls.count { it.table == SyncTable.CATEGORIES }) // a full page, then a short one
    }

    @Test
    fun pulledRowsAreNotUploadedAgain() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        remote.rows.getValue(SyncTable.KNOWLEDGE_ITEMS) += itemRow("i1")

        run()

        assertTrue(store.outbox.isEmpty())
        assertTrue(remote.allPushed.isEmpty())
    }

    @Test
    fun mergeResultsAreUploadedInTheSameRun() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        store.addCategory("local", "Science", created = 20)
        store.addItem("i1", categoryId = "local", updated = 100)
        remote.rows.getValue(SyncTable.CATEGORIES) += catRow("remote", "Science", created = 10)

        val result = run() as SyncRunResult.Success

        val sent = remote.allPushed.map { it.type to it.operation }
        assertTrue(sent.contains(SyncEntityType.KNOWLEDGE_ITEM to SyncOperation.UPSERT))
        assertTrue(sent.contains(SyncEntityType.CATEGORY to SyncOperation.DELETE))
        assertEquals(0, result.remaining)
    }

    @Test
    fun failedPullDoesNotAdvanceTheCursor() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        remote.rows.getValue(SyncTable.CATEGORIES) += catRow("c1", "One", server = "2026-01-01T00:00:01Z")
        remote.failure = SyncFailureKind.SERVER

        run()

        assertNull(store.state[SyncStateKeys.cursor(SyncTable.CATEGORIES)])
    }
}