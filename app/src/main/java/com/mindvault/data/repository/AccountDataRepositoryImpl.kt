package com.mindvault.data.repository

import android.util.Log
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.repository.AccountDataRepository
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AccountData"

@Singleton
class AccountDataRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val active: ActiveDatabase,
) : AccountDataRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override val owner: StateFlow<DataOwner> get() = active.owner

    @Synchronized
    override fun start() {
        if (job != null) return
        job = scope.launch {
            auth.observeSessionState()
                .catch { Log.e(TAG, "Session updates stopped (${it::class.simpleName})") }
                .collect { state -> ownerFor(state)?.let(active::switchTo) }
        }
    }

    override suspend fun deleteAccountData(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                active.deleteAccountDatabase(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The server account is already gone; a leftover file is never reused (new account, new ID).
                Log.w(TAG, "Could not delete the local vault (${e::class.simpleName})")
            }
        }
    }

    /** Loading and Expired keep the current vault, so a slow start or a rejected session never swaps data. */
    private fun ownerFor(state: SessionState): DataOwner? = when (state) {
        SessionState.Loading -> null
        SessionState.Expired -> null
        SessionState.SignedOut -> DataOwner.Guest
        is SessionState.SignedIn -> DataOwner.Account(state.user.id)
    }
}