package com.mindvault.data.sync

/**
 * The cloud side of sync (the sync_push and sync_pull functions from docs/supabase_sync_schema.sql).
 * Implementations must throw [SyncRemoteException] for network, auth and server problems, and must never
 * send or fetch data for any user other than [userId].
 */
interface SyncRemote {
    suspend fun push(userId: String, changes: List<PushChange>): PushResponse

    /** One page of rows changed after ([since], [afterId]), oldest first. [since] is ISO-8601 UTC. */
    suspend fun pull(userId: String, table: SyncTable, since: String, afterId: String, limit: Int): List<SyncRow>
}