package com.mindvault.data.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindvault.domain.model.NotificationPlan
import com.mindvault.domain.repository.NotificationSettingsRepository
import com.mindvault.domain.usecase.GetLastSavedAtUseCase
import com.mindvault.domain.usecase.NotificationPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/** Runs at 18:00 each day: posts a come-back line when the planner allows it, then schedules tomorrow's run. */
@HiltWorker
class NudgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: NotificationSettingsRepository,
    private val getLastSavedAt: GetLastSavedAtUseCase,
    private val helper: NotificationHelper,
    private val scheduler: NotificationScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.get()
        try {
            if (settings.nudgesEnabled) {
                val plan = NotificationPlanner.planNudge(
                    lastSavedAt = getLastSavedAt(),
                    now = System.currentTimeMillis(),
                    lineCount = helper.nudgeLineCount(),
                    settings = settings,
                )
                if (plan is NotificationPlan.Nudge) {
                    helper.showNudge(plan.lineIndex)
                    settingsRepository.recordNudge(LocalDate.now().toEpochDay(), plan.lineIndex)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not plan a nudge (${e::class.simpleName})")
        }
        if (settings.nudgesEnabled) scheduler.scheduleNextNudge()
        return Result.success()
    }

    private companion object {
        const val TAG = "NudgeWorker"
    }
}