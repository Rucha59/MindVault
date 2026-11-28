package com.mindvault.data.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindvault.domain.model.NotificationPlan
import com.mindvault.domain.repository.NotificationSettingsRepository
import com.mindvault.domain.usecase.CountDueCardsUseCase
import com.mindvault.domain.usecase.NotificationPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

/** Runs at the chosen time each day: posts "N cards are due" only if cards are due, then schedules tomorrow's run. */
@HiltWorker
class DueReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: NotificationSettingsRepository,
    private val countDueCards: CountDueCardsUseCase,
    private val helper: NotificationHelper,
    private val scheduler: NotificationScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.get()
        try {
            if (settings.dueRemindersEnabled) {
                val plan = NotificationPlanner.planDueReminder(countDueCards(), settings)
                if (plan is NotificationPlan.DueReminder) helper.showDueReminder(plan.count)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not check due cards (${e::class.simpleName})")
        }
        if (settings.dueRemindersEnabled) scheduler.scheduleNextDue(settings.reminderMinuteOfDay)
        return Result.success()
    }

    private companion object {
        const val TAG = "DueReminderWorker"
    }
}