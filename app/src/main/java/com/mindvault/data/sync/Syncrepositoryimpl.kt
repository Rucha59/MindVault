package com.mindvault.data.sync

import android.util.Log
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.SyncProblem
import com.mindvault.domain.model.SyncStatus
import com.mindvault.domain.repository.AuthRepository
import com.mindvault.domain.repository.ScheduledSyncResult
import com.mindvault.domain.repository.SyncRepository
import com.mindvault.domain.usecase.SyncStatusBuilder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "SyncRepository"

/**
 * Runs the [SyncEngine] on the open account vault, one run at a time, and reports honest status.
 * The guest vault never syncs. Sync only runs while the signed-in user is the owner of the open vault.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val active: ActiveDatabase,
    private val auth: AuthRepository,
    remote: SyncRemote,
    private val scheduler: SyncScheduler,
) : SyncRepository {

    private val engine = SyncEngine(remote)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val running = MutableStateFlow(false)
    private var started = false

    override val status: Flow<SyncStatus> = active.owner.flatMapLatest { owner ->
        if (owner !is DataOwner.Account) {
            flowOf(SyncStatus.Inactive)
        } else {
            combine(
                running,
                active.observe { db -> db.syncOutboxDao().observeCount() },
                active.observe { db -> db.syncStateDao().observe(SyncStateKeys.LAST_SUCCESS) },
                active.observe { db -> db.syncStateDao().observe(SyncStateKeys.LAST_PROBLEM) },
            ) { syncing, pending, lastSuccess, problem ->
                SyncStatusBuilder.build(
                    syncing = syncing,
                    pendingChanges = pending,
                    lastSuccessAt = lastSuccess?.toLongOrNull(),
                    problem = problem?.let { name -> SyncProblem.entries.firstOrNull { it.name == name } },
                )
            }
        }
    }

    @Synchronized
    override fun start() {
        if (started) return
        started = true
        // App open and account change: schedule the periodic run and sync soon. Guest: stop everything.
        scope.launch {
            active.owner.collect { owner ->
                try {
                    if (owner is DataOwner.Account) {
                        scheduler.schedulePeriodic()
                        scheduler.requestNow()
                    } else {
                        scheduler.cancelAll()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not schedule sync (${e::class.simpleName})")
                }
            }
        }
        // A few seconds after the last local change, sync. Several quick edits cause one sync.
        scope.launch {
            active.observe { db -> db.syncOutboxDao().observeCount() }
                .distinctUntilChanged()
                .debounce(CHANGE_DELAY_MS)
                .filter { it > 0 }
                .collect {
                    try {
                        if (active.owner.value is DataOwner.Account) scheduler.requestNow()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not schedule sync (${e::class.simpleName})")
                    }
                }
        }
    }

    override suspend fun syncNow() {
        mutex.withLock { syncLocked() }
    }

    override suspend fun runScheduledSync(): ScheduledSyncResult {
        return when (val result = mutex.withLock { syncLocked() }) {
            null, is SyncRunResult.Success -> ScheduledSyncResult.DONE
            is SyncRunResult.Failure -> when (result.kind) {
                SyncFailureKind.AUTH -> ScheduledSyncResult.DONE // retrying cannot fix a missing sign-in
                SyncFailureKind.NETWORK, SyncFailureKind.SERVER, SyncFailureKind.UNKNOWN -> ScheduledSyncResult.RETRY
            }
        }
    }

    override suspend fun <T> withFinalSync(block: suspend (fullyUploaded: Boolean) -> T): T = mutex.withLock {
        val result = withTimeoutOrNull(FINAL_SYNC_TIMEOUT_MS) { syncLocked() }
        val fullyUploaded = result is SyncRunResult.Success && result.remaining == 0 && result.rejected == 0
        block(fullyUploaded)
    }

    /** Must be called with [mutex] held. Returns null when there is no account vault to sync. */
    private suspend fun syncLocked(): SyncRunResult? {
        val owner = active.owner.value as? DataOwner.Account ?: return null
        // At app start the session may still be loading; wait for it instead of reporting a false sign-in problem.
        val session = withTimeoutOrNull(SESSION_WAIT_MS) {
            auth.observeSessionState().first { it !is SessionState.Loading }
        }
        val db = active.current
        if (active.owner.value != owner) return null // the account changed while waiting
        val store = RoomSyncLocalStore(db)

        val result: SyncRunResult = if (session !is SessionState.SignedIn || session.user.id != owner.userId) {
            SyncRunResult.Failure(SyncFailureKind.AUTH, remainingOf(store))
        } else {
            running.value = true
            try {
                engine.run(store, owner.userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed (${e::class.simpleName})") // type only: messages can contain user data
                SyncRunResult.Failure(SyncFailureKind.UNKNOWN, remainingOf(store))
            } finally {
                running.value = false
            }
        }
        record(store, result)
        return result
    }

    private suspend fun record(store: SyncLocalStore, result: SyncRunResult) {
        try {
            when (result) {
                is SyncRunResult.Success -> {
                    store.statePut(SyncStateKeys.LAST_SUCCESS, System.currentTimeMillis().toString())
                    if (result.rejected > 0) {
                        store.statePut(SyncStateKeys.LAST_PROBLEM, SyncProblem.REJECTED.name)
                    } else {
                        store.stateRemove(SyncStateKeys.LAST_PROBLEM)
                    }
                }
                is SyncRunResult.Failure ->
                    store.statePut(SyncStateKeys.LAST_PROBLEM, problemFor(result.kind).name)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not record the sync result (${e::class.simpleName})")
        }
    }

    private suspend fun remainingOf(store: SyncLocalStore): Int =
        try { store.outboxAll().size } catch (e: Exception) { 0 }

    private fun problemFor(kind: SyncFailureKind): SyncProblem = when (kind) {
        SyncFailureKind.NETWORK -> SyncProblem.OFFLINE
        SyncFailureKind.AUTH -> SyncProblem.SIGN_IN_NEEDED
        SyncFailureKind.SERVER -> SyncProblem.SERVER
        SyncFailureKind.UNKNOWN -> SyncProblem.UNKNOWN
    }

    private companion object {
        const val CHANGE_DELAY_MS = 5_000L
        const val SESSION_WAIT_MS = 10_000L
        const val FINAL_SYNC_TIMEOUT_MS = 25_000L
    }
}