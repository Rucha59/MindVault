package com.mindvault.data.sync

import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.SyncOutboxEntity
import com.mindvault.data.local.entity.outboxEntry
import java.time.Duration
import java.time.Instant

/**
 * One sync run for one vault: upload pending changes, download changes, resolve conflicts, upload what
 * conflict resolution changed. It knows nothing about Supabase, Room or Android: it talks to [SyncRemote] and
 * [SyncLocalStore]. Callers must make sure [userId] is the account that owns the vault behind [store].
 */
class SyncEngine(
    private val remote: SyncRemote,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val pageSize: Int = PAGE_SIZE,
    private val batchSize: Int = BATCH_SIZE,
) {
    private class Stats {
        var uploaded = 0
        var downloaded = 0
        val rejected = HashSet<String>()
    }

    /** Sends parents before children and deletions last, so one run can upload a whole new tree. */
    private class Phase(val operation: String, val types: List<String>)

    suspend fun run(store: SyncLocalStore, userId: String): SyncRunResult {
        val stats = Stats()
        return try {
            enqueueInitialUpload(store)
            pushAll(store, userId, stats)
            val applier = SyncApplier(store, now)
            for (table in SyncTable.entries) pullTable(store, userId, table, applier, stats)
            // Merges and renames made while pulling are uploaded straight away.
            if (applier.enqueued > 0) pushAll(store, userId, stats)
            SyncRunResult.Success(stats.uploaded, stats.downloaded, stats.rejected.size, store.outboxAll().size)
        } catch (e: SyncRemoteException) {
            SyncRunResult.Failure(e.kind, remaining(store))
        }
    }

    private suspend fun remaining(store: SyncLocalStore): Int =
        try { store.outboxAll().size } catch (e: Exception) { 0 }

    // ---------------------------------------------------------------- first full upload

    /**
     * Rows that existed before sync was added have no outbox entry. The first run queues all of them once;
     * rows that already have a pending entry keep it.
     */
    private suspend fun enqueueInitialUpload(store: SyncLocalStore) {
        if (store.stateGet(SyncStateKeys.INITIAL_UPLOAD) == "1") return
        store.inTransaction {
            for (c in store.categories()) store.outboxPutIfAbsent(upsertEntry(SyncEntityType.CATEGORY, c.id, c.updatedAt))
            for (d in store.decks()) store.outboxPutIfAbsent(upsertEntry(SyncEntityType.DECK, d.id, d.updatedAt))
            for (i in store.items()) store.outboxPutIfAbsent(upsertEntry(SyncEntityType.KNOWLEDGE_ITEM, i.id, i.updatedAt))
            for (f in store.flashcards()) store.outboxPutIfAbsent(upsertEntry(SyncEntityType.FLASHCARD, f.id, f.createdAt))
            for (r in store.reviewStates()) store.outboxPutIfAbsent(upsertEntry(SyncEntityType.REVIEW_STATE, r.flashcardId, r.updatedAt))
            store.statePut(SyncStateKeys.INITIAL_UPLOAD, "1")
        }
    }

    private fun upsertEntry(type: String, id: String, changedAt: Long) =
        outboxEntry(type, id, SyncOperation.UPSERT, changedAt)

    // ---------------------------------------------------------------- push

    private suspend fun pushAll(store: SyncLocalStore, userId: String, stats: Stats) {
        var pass = 0
        while (pass++ < MAX_PUSH_PASSES) {
            if (store.outboxAll().isEmpty()) return
            var handled = 0
            for (phase in PHASES) handled += pushPhase(store, userId, phase, stats)
            // Another pass picks up changes made while uploading; it stops when nothing more could be sent.
            if (handled == 0) return
        }
    }

    /** Returns how many outbox entries were finished (uploaded, or dropped because their row is gone). */
    private suspend fun pushPhase(store: SyncLocalStore, userId: String, phase: Phase, stats: Stats): Int {
        val all = store.outboxAll()
        val pendingUpserts = all.filter { it.operation == SyncOperation.UPSERT }
            .mapTo(HashSet()) { outboxKey(it.entityType, it.entityId) }
        val entries = all.filter { it.operation == phase.operation && it.entityType in phase.types }
            .sortedWith(compareBy({ phase.types.indexOf(it.entityType) }, { it.changedAt }, { it.entityId }))

        var handled = 0
        val batch = ArrayList<Pair<SyncOutboxEntity, PushChange>>()
        for (entry in entries) {
            if (entry.operation == SyncOperation.DELETE) {
                batch += entry to PushChange(entry.entityType, entry.operation, entry.entityId, entry.changedAt, null)
                continue
            }
            val data = store.upsertData(entry.entityType, entry.entityId)
            if (data == null) {
                // The row was deleted after the change was recorded; its deletion has its own entry.
                store.outboxRemoveIfUnchanged(entry)
                handled++
                continue
            }
            // The server skips a card whose item is not there yet, without telling us. So a row waits until
            // everything it depends on has been uploaded.
            if (dependencies(entry, data).any { it in pendingUpserts }) continue
            batch += entry to PushChange(entry.entityType, entry.operation, entry.entityId, entry.changedAt, data)
        }

        for (chunk in batch.chunked(batchSize)) {
            val response = remote.push(userId, chunk.map { it.second })
            for ((entry, _) in chunk) {
                val key = outboxKey(entry.entityType, entry.entityId)
                if (key in response.rejected) {
                    stats.rejected += key
                } else {
                    // A newer change recorded meanwhile keeps its entry and goes out in the next pass.
                    store.outboxRemoveIfUnchanged(entry)
                    stats.rejected -= key
                    stats.uploaded++
                    handled++
                }
            }
        }
        return handled
    }

    private fun dependencies(entry: SyncOutboxEntity, data: Map<String, Any?>): List<String> = when (entry.entityType) {
        SyncEntityType.KNOWLEDGE_ITEM -> listOfNotNull((data["category_id"] as? String)?.let { outboxKey(SyncEntityType.CATEGORY, it) })
        SyncEntityType.FLASHCARD -> listOfNotNull(
            (data["knowledge_item_id"] as? String)?.let { outboxKey(SyncEntityType.KNOWLEDGE_ITEM, it) },
            (data["deck_id"] as? String)?.let { outboxKey(SyncEntityType.DECK, it) },
        )
        SyncEntityType.REVIEW_STATE -> listOf(outboxKey(SyncEntityType.FLASHCARD, entry.entityId))
        else -> emptyList()
    }

    // ---------------------------------------------------------------- pull

    private suspend fun pullTable(
        store: SyncLocalStore,
        userId: String,
        table: SyncTable,
        applier: SyncApplier,
        stats: Stats,
    ) {
        val cursorKey = SyncStateKeys.cursor(table)
        val saved = store.stateGet(cursorKey)
        // Rows from a slow transaction can commit after a later one, so every run starts a few minutes back.
        // Applying a row twice is harmless: an equal updatedAt is ignored.
        var since = saved?.let { Instant.parse(it).minus(REREAD_WINDOW).toString() } ?: EPOCH
        var afterId = ""
        var newest = saved
        while (true) {
            val rows = remote.pull(userId, table, since, afterId, pageSize)
            if (rows.isEmpty()) break
            store.inTransaction { for (row in rows) applier.apply(table, row) }
            stats.downloaded += rows.size
            val last = rows.last()
            val lastServerTime = last.str("server_updated_at") ?: break
            val lastId = last.str("id") ?: break
            since = lastServerTime
            afterId = lastId
            if (newest == null || Instant.parse(lastServerTime) > Instant.parse(newest)) newest = lastServerTime
            if (rows.size < pageSize) break
        }
        if (newest != null && newest != saved) store.statePut(cursorKey, newest)
    }

    companion object {
        const val PAGE_SIZE = 500
        const val BATCH_SIZE = 500
        private const val MAX_PUSH_PASSES = 5
        private const val EPOCH = "1970-01-01T00:00:00Z"
        private val REREAD_WINDOW: Duration = Duration.ofMinutes(5)

        private val PHASES = listOf(
            Phase(SyncOperation.UPSERT, listOf(SyncEntityType.CATEGORY, SyncEntityType.DECK)),
            Phase(SyncOperation.UPSERT, listOf(SyncEntityType.KNOWLEDGE_ITEM)),
            Phase(SyncOperation.UPSERT, listOf(SyncEntityType.FLASHCARD)),
            Phase(SyncOperation.UPSERT, listOf(SyncEntityType.REVIEW_STATE)),
            Phase(SyncOperation.DELETE, listOf(SyncEntityType.FLASHCARD)),
            Phase(SyncOperation.DELETE, listOf(SyncEntityType.KNOWLEDGE_ITEM)),
            Phase(SyncOperation.DELETE, listOf(SyncEntityType.DECK)),
            Phase(SyncOperation.DELETE, listOf(SyncEntityType.CATEGORY)),
        )
    }
}