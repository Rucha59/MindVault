package com.mindvault.domain.usecase

import com.mindvault.domain.model.SyncProblem
import com.mindvault.domain.model.SyncStatus

/** Turns the raw facts about a vault into the one status the user sees. Pure, so it is unit tested. */
object SyncStatusBuilder {
    fun build(syncing: Boolean, pendingChanges: Int, lastSuccessAt: Long?, problem: SyncProblem?): SyncStatus = when {
        syncing -> SyncStatus.Syncing
        problem != null -> SyncStatus.NeedsAttention(problem, pendingChanges, lastSuccessAt)
        pendingChanges > 0 -> SyncStatus.Waiting(pendingChanges)
        lastSuccessAt != null -> SyncStatus.Synced(lastSuccessAt)
        else -> SyncStatus.NotSyncedYet
    }
}