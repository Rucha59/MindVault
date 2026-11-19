package com.mindvault.domain.model

enum class SyncProblem { OFFLINE, SIGN_IN_NEEDED, SERVER, REJECTED, UNKNOWN }

/** What the Profile account card shows. Only describes things that are actually true right now. */
sealed interface SyncStatus {
    /** Guest, or no account vault open: nothing syncs and nothing is shown. */
    data object Inactive : SyncStatus

    data object Syncing : SyncStatus

    /** Signed in, nothing waiting, but no sync has completed on this device yet. */
    data object NotSyncedYet : SyncStatus

    /** Changes saved on this device that have not been uploaded yet. */
    data class Waiting(val pendingChanges: Int) : SyncStatus

    /** The last sync finished and nothing is waiting. [at] is epoch millis. */
    data class Synced(val at: Long) : SyncStatus

    /** The last attempt failed or some changes were refused. [lastSyncedAt] is null if never synced. */
    data class NeedsAttention(
        val problem: SyncProblem,
        val pendingChanges: Int,
        val lastSyncedAt: Long?,
    ) : SyncStatus
}