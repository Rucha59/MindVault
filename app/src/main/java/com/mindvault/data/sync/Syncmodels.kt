package com.mindvault.data.sync

/** One row as returned by sync_pull: column name -> String / Number / Boolean / null. */
typealias SyncRow = Map<String, Any?>

internal fun SyncRow.str(key: String): String? = this[key] as? String
internal fun SyncRow.long(key: String): Long? = (this[key] as? Number)?.toLong()
internal fun SyncRow.int(key: String): Int? = (this[key] as? Number)?.toInt()
internal fun SyncRow.double(key: String): Double? = (this[key] as? Number)?.toDouble()
internal fun SyncRow.bool(key: String): Boolean? = this[key] as? Boolean

/** The cloud tables, in the order they are pulled (parents before children). [wire] is the sync_pull name. */
enum class SyncTable(val wire: String) {
    CATEGORIES("categories"),
    DECKS("decks"),
    KNOWLEDGE_ITEMS("knowledge_items"),
    FLASHCARDS("flashcards"),
    REVIEW_STATES("review_states"),
    REVIEW_LOGS("review_logs"),
}

enum class SyncFailureKind { NETWORK, AUTH, SERVER, UNKNOWN }

/** The only exception the engine expects from a [SyncRemote]. Everything else is a bug and propagates. */
class SyncRemoteException(val kind: SyncFailureKind, cause: Throwable? = null) : Exception(kind.name, cause)

/** One change for sync_push. [data] is null for DELETE. All timestamps are epoch milliseconds. */
data class PushChange(
    val type: String,
    val operation: String,
    val id: String,
    val changedAt: Long,
    val data: Map<String, Any?>?,
)

/** [rejected] holds `"<type>|<id>"` keys of rows the server could not apply. */
data class PushResponse(val rejected: Set<String>)

sealed interface SyncRunResult {
    data class Success(val uploaded: Int, val downloaded: Int, val rejected: Int, val remaining: Int) : SyncRunResult
    data class Failure(val kind: SyncFailureKind, val remaining: Int) : SyncRunResult
}

/** Keys used in the sync_state table. */
object SyncStateKeys {
    const val INITIAL_UPLOAD = "initial_upload_done"
    const val LAST_SUCCESS = "last_success_at"
    const val LAST_PROBLEM = "last_problem"

    fun cursor(table: SyncTable) = "cursor.${table.wire}"

    /** A category that lost a same-name merge points at the one that was kept. */
    fun categoryAlias(categoryId: String) = "catalias.$categoryId"
}

internal fun outboxKey(type: String, id: String) = "$type|$id"