package com.mindvault.data.sync

import com.mindvault.data.local.entity.ReviewLogEntity
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun logRow(
    id: String,
    flashcardId: String = "c1",
    rating: String = "GOOD",
    reviewedAt: Long = 1_000L,
    server: String = T0,
) = mapOf(
    "id" to id, "flashcard_id" to flashcardId, "rating" to rating, "reviewed_at" to reviewedAt,
    "interval_days_before" to 0, "interval_days_after" to 2, "server_updated_at" to server,
)

/** The review history is append-only: uploaded once, downloaded once, never edited, independent of its card. */
class SyncReviewLogTest {
    private val store = FakeSyncLocalStore()
    private val remote = FakeSyncRemote()
    private val engine = SyncEngine(remote, now = { 5_000L }, pageSize = 2)

    private fun run() = runBlocking { engine.run(store, "user-1") } as SyncRunResult.Success

    private fun addLog(id: String, flashcardId: String = "c1") {
        store.logRows[id] = ReviewLogEntity(id, flashcardId, "GOOD", 1_000L, 0, 2)
    }

    private fun queueLog(id: String, at: Long = 1L) {
        store.outbox[outboxKey(SyncEntityType.REVIEW_LOG, id)] =
            outboxEntry(SyncEntityType.REVIEW_LOG, id, SyncOperation.UPSERT, at)
    }

    // ---- applier

    @Test
    fun pulledLogIsInsertedWithoutAnOutboxEntry() {
        val applier = SyncApplier(store) { 1_000L }
        runBlocking { applier.apply(SyncTable.REVIEW_LOGS, logRow("l1")) }

        val log = store.logRows.getValue("l1")
        assertEquals("GOOD", log.rating)
        assertEquals(2, log.intervalDaysAfter)
        assertTrue(store.outbox.isEmpty())
        assertEquals(0, applier.enqueued)
    }

    @Test
    fun anExistingLogIsNeverChanged() {
        store.logRows["l1"] = ReviewLogEntity("l1", "c1", "HARD", 5L, 1, 1)
        val applier = SyncApplier(store) { 1_000L }
        runBlocking { applier.apply(SyncTable.REVIEW_LOGS, logRow("l1", rating = "GOOD", reviewedAt = 9L)) }

        assertEquals("HARD", store.logRows.getValue("l1").rating)
        assertEquals(5L, store.logRows.getValue("l1").reviewedAt)
    }

    @Test
    fun aLogWithAnUnknownRatingOrMissingFieldIsIgnored() {
        val applier = SyncApplier(store) { 1_000L }
        runBlocking {
            applier.apply(SyncTable.REVIEW_LOGS, logRow("bad", rating = "PERFECT"))
            applier.apply(SyncTable.REVIEW_LOGS, logRow("incomplete").minus("reviewed_at"))
        }
        assertTrue(store.logRows.isEmpty())
    }

    @Test
    fun aLogWithTheAgainRatingIsIgnoredBecauseAgainIsNotLogged() {
        val applier = SyncApplier(store) { 1_000L }
        runBlocking { applier.apply(SyncTable.REVIEW_LOGS, logRow("l1", rating = "AGAIN")) }
        assertTrue(store.logRows.isEmpty())
    }

    @Test
    fun aLogIsKeptEvenWhenItsCardIsGone() {
        val applier = SyncApplier(store) { 1_000L }
        runBlocking { applier.apply(SyncTable.REVIEW_LOGS, logRow("l1", flashcardId = "deleted-card")) }
        assertNotNull(store.logRows["l1"])
    }

    // ---- engine

    @Test
    fun queuedLogIsUploadedWithItsDataAndRemovedFromTheOutbox() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        addLog("l1")
        queueLog("l1")

        val result = run()

        val change = remote.allPushed.single { it.type == SyncEntityType.REVIEW_LOG }
        assertEquals("l1", change.id)
        assertEquals(SyncOperation.UPSERT, change.operation)
        assertEquals("c1", change.data?.get("flashcard_id"))
        assertEquals("GOOD", change.data?.get("rating"))
        assertEquals(1_000L, change.data?.get("reviewed_at"))
        assertEquals(0, change.data?.get("interval_days_before"))
        assertEquals(2, change.data?.get("interval_days_after"))
        assertEquals(0, result.remaining)
        assertTrue(store.outbox.isEmpty())
    }

    @Test
    fun aLogDoesNotWaitForItsCard() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        addLog("l1", flashcardId = "card-that-is-gone")
        queueLog("l1")

        run()

        assertEquals(1, remote.allPushed.count { it.type == SyncEntityType.REVIEW_LOG })
    }

    @Test
    fun aQueuedLogThatNoLongerExistsIsDroppedWithoutBeingSent() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        queueLog("missing")

        val result = run()

        assertTrue(remote.allPushed.isEmpty())
        assertEquals(0, result.remaining)
    }

    @Test
    fun aRejectedLogStaysQueued() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        addLog("l1")
        queueLog("l1")
        remote.rejected = setOf(outboxKey(SyncEntityType.REVIEW_LOG, "l1"))

        val result = run()

        assertEquals(1, result.rejected)
        assertNotNull(store.outbox[outboxKey(SyncEntityType.REVIEW_LOG, "l1")])
    }

    @Test
    fun pullReadsEveryPageOfLogsAndSavesTheCursor() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        remote.rows.getValue(SyncTable.REVIEW_LOGS) += listOf(
            logRow("l1", server = "2026-01-01T00:00:01Z"),
            logRow("l2", server = "2026-01-01T00:00:02Z"),
            logRow("l3", server = "2026-01-01T00:00:03Z"),
        )

        val result = run()

        assertEquals(setOf("l1", "l2", "l3"), store.logRows.keys)
        assertEquals(3, result.downloaded)
        assertEquals("2026-01-01T00:00:03Z", store.state[SyncStateKeys.cursor(SyncTable.REVIEW_LOGS)])
        assertTrue("pulled logs are not uploaded again", remote.allPushed.isEmpty())
    }

    @Test
    fun aLogUploadedByThisDeviceAndPulledBackIsNotDuplicated() {
        store.state[SyncStateKeys.INITIAL_UPLOAD] = "1"
        addLog("l1")
        remote.rows.getValue(SyncTable.REVIEW_LOGS) += logRow("l1")

        run()

        assertEquals(1, store.logRows.size)
        assertNull(store.outbox[outboxKey(SyncEntityType.REVIEW_LOG, "l1")])
    }
}