package com.mindvault.domain.repository

import com.mindvault.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

enum class ScheduledSyncResult { DONE, RETRY }

interface SyncRepository {
    val status: Flow<SyncStatus>

    /** Starts the triggers (app open, a few seconds after a change, every ~6 hours). Safe to call twice. */
    fun start()

    /** Syncs now, in the foreground. Does nothing for the guest vault. Progress and result show in [status]. */
    suspend fun syncNow()

    /** The background run used by WorkManager. [ScheduledSyncResult.RETRY] asks for a retry with backoff. */
    suspend fun runScheduledSync(): ScheduledSyncResult

    /**
     * Syncs one last time and then runs [block], with sync paused for the whole call.
     * [block] receives true only when every local change has reached the cloud.
     */
    suspend fun <T> withFinalSync(block: suspend (fullyUploaded: Boolean) -> T): T
}