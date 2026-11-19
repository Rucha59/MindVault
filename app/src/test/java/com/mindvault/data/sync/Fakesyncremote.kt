package com.mindvault.data.sync

import java.time.Instant

/** A cloud stand-in: records every push, returns canned pull rows with real keyset paging. */
class FakeSyncRemote : SyncRemote {
    data class PullCall(val table: SyncTable, val since: String, val afterId: String, val limit: Int)

    val pushes = mutableListOf<List<PushChange>>()
    val pullCalls = mutableListOf<PullCall>()
    val rows = SyncTable.entries.associateWith { mutableListOf<SyncRow>() }
    var rejected: Set<String> = emptySet()
    var failure: SyncFailureKind? = null
    var onPush: (suspend () -> Unit)? = null

    val allPushed: List<PushChange> get() = pushes.flatten()

    override suspend fun push(userId: String, changes: List<PushChange>): PushResponse {
        failure?.let { throw SyncRemoteException(it) }
        onPush?.also { onPush = null }?.invoke()
        pushes += changes
        return PushResponse(rejected)
    }

    override suspend fun pull(userId: String, table: SyncTable, since: String, afterId: String, limit: Int): List<SyncRow> {
        failure?.let { throw SyncRemoteException(it) }
        pullCalls += PullCall(table, since, afterId, limit)
        val sinceInstant = Instant.parse(since)
        return rows.getValue(table)
            .sortedWith(compareBy({ Instant.parse(it.str("server_updated_at")!!) }, { it.str("id")!! }))
            .filter {
                val t = Instant.parse(it.str("server_updated_at")!!)
                t > sinceInstant || (t == sinceInstant && it.str("id")!! > afterId)
            }
            .take(limit)
    }
}

const val T0 = "2026-01-01T00:00:00Z"

fun catRow(id: String, name: String, created: Long = 10, updated: Long = created, deleted: Long? = null, server: String = T0) =
    mapOf("id" to id, "name" to name, "created_at" to created, "updated_at" to updated, "deleted_at" to deleted, "server_updated_at" to server)

fun deckRow(id: String, name: String, created: Long = 10, updated: Long = created, deleted: Long? = null, isDefault: Boolean = false, server: String = T0) =
    mapOf("id" to id, "name" to name, "is_default" to isDefault, "created_at" to created, "updated_at" to updated, "deleted_at" to deleted, "server_updated_at" to server)

fun itemRow(id: String, title: String = "Remote $id", category: String? = null, created: Long = 10, updated: Long = created, deleted: Long? = null, server: String = T0) =
    mapOf(
        "id" to id, "title" to title, "type" to "WORD", "definition" to "Remote definition", "personal_notes" to "",
        "example" to null, "category_id" to category, "created_at" to created, "updated_at" to updated,
        "deleted_at" to deleted, "server_updated_at" to server,
    )

fun cardRow(id: String, itemId: String, deckId: String, created: Long = 10, deleted: Long? = null, server: String = T0) =
    mapOf("id" to id, "knowledge_item_id" to itemId, "deck_id" to deckId, "created_at" to created, "deleted_at" to deleted, "server_updated_at" to server)

fun stateRow(cardId: String, updated: Long, reviewCount: Int = 1, server: String = T0) =
    mapOf(
        "id" to cardId, "last_reviewed_at" to updated, "next_review_at" to updated + 1000, "review_count" to reviewCount,
        "ease_factor" to 2.5, "interval_days" to 1, "updated_at" to updated, "server_updated_at" to server,
    )