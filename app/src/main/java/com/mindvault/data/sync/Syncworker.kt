package com.mindvault.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindvault.domain.repository.ScheduledSyncResult
import com.mindvault.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (sync.runScheduledSync()) {
        ScheduledSyncResult.DONE -> Result.success()
        // WorkManager backs off exponentially between retries; give up after a few so a broken server
        // is not hammered. The periodic run and the next change try again.
        ScheduledSyncResult.RETRY -> if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}