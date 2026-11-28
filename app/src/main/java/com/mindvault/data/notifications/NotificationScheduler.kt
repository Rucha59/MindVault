package com.mindvault.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mindvault.domain.repository.NotificationSettingsRepository
import com.mindvault.domain.usecase.NotificationPlanner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps one WorkManager job per notification kind scheduled for its next time. Each worker schedules the next run
 * itself when it finishes. [start] runs at app start (keeps jobs that are already waiting) and again whenever the
 * switches or the reminder time change (replaces them).
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: NotificationSettingsRepository,
    private val helper: NotificationHelper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val workManager get() = WorkManager.getInstance(context)

    fun start() {
        helper.ensureChannels()
        scope.launch {
            settings.ensureFirstSeen(LocalDate.now().toEpochDay())
            var first = true
            settings.observe()
                .map { Triple(it.dueRemindersEnabled, it.reminderMinuteOfDay, it.nudgesEnabled) }
                .distinctUntilChanged()
                .collect { (due, minute, nudges) ->
                    val policy = if (first) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE
                    first = false
                    if (due) enqueue<DueReminderWorker>(DUE_NAME, minute, policy) else workManager.cancelUniqueWork(DUE_NAME)
                    if (nudges) {
                        enqueue<NudgeWorker>(NUDGE_NAME, NotificationPlanner.NUDGE_MINUTE_OF_DAY, policy)
                    } else {
                        workManager.cancelUniqueWork(NUDGE_NAME)
                    }
                }
        }
    }

    /** Called by the worker as its last step; REPLACE is safe because the work is already done. */
    fun scheduleNextDue(minuteOfDay: Int) =
        enqueue<DueReminderWorker>(DUE_NAME, minuteOfDay, ExistingWorkPolicy.REPLACE)

    fun scheduleNextNudge() =
        enqueue<NudgeWorker>(NUDGE_NAME, NotificationPlanner.NUDGE_MINUTE_OF_DAY, ExistingWorkPolicy.REPLACE)

    private inline fun <reified W : ListenableWorker> enqueue(name: String, minuteOfDay: Int, policy: ExistingWorkPolicy) {
        val delay = NotificationPlanner.delayUntilNextMillis(System.currentTimeMillis(), minuteOfDay)
        val request = OneTimeWorkRequestBuilder<W>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        workManager.enqueueUniqueWork(name, policy, request)
    }

    private companion object {
        const val DUE_NAME = "mindvault-notify-due"
        const val NUDGE_NAME = "mindvault-notify-nudge"
    }
}