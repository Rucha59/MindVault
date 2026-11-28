package com.mindvault.domain.usecase

import com.mindvault.domain.model.NotificationPlan
import com.mindvault.domain.model.NotificationSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Pure notification rules (no Android, no I/O), so they are easy to unit test. */
object NotificationPlanner {
    const val NUDGE_AFTER_DAYS_WITHOUT_SAVE = 3L
    const val NUDGE_MIN_DAYS_BETWEEN = 2L

    /** The come-back nudge is sent at 18:00 local time. */
    const val NUDGE_MINUTE_OF_DAY = 18 * 60

    /** A due reminder is sent only when the reminder is on and cards are actually due. */
    fun planDueReminder(dueCount: Int, settings: NotificationSettings): NotificationPlan =
        if (settings.dueRemindersEnabled && dueCount > 0) NotificationPlan.DueReminder(dueCount) else NotificationPlan.None

    /**
     * A nudge is sent when nothing was saved for [NUDGE_AFTER_DAYS_WITHOUT_SAVE] days (counted from the later of the
     * last save and the day notifications were first scheduled) and at least [NUDGE_MIN_DAYS_BETWEEN] days have
     * passed since the last nudge. Lines go round in order, so the same line never comes twice in a row.
     */
    fun planNudge(
        lastSavedAt: Long?,
        now: Long,
        lineCount: Int,
        settings: NotificationSettings,
        zone: ZoneId = ZoneId.systemDefault(),
    ): NotificationPlan {
        if (!settings.nudgesEnabled || lineCount <= 0) return NotificationPlan.None
        val today = epochDay(now, zone)
        val reference = listOfNotNull(lastSavedAt?.let { epochDay(it, zone) }, settings.firstSeenEpochDay).maxOrNull()
            ?: return NotificationPlan.None
        if (today - reference < NUDGE_AFTER_DAYS_WITHOUT_SAVE) return NotificationPlan.None
        val lastNudge = settings.lastNudgeEpochDay
        if (lastNudge != null && today - lastNudge < NUDGE_MIN_DAYS_BETWEEN) return NotificationPlan.None
        return NotificationPlan.Nudge((settings.lastNudgeLine + 1).mod(lineCount))
    }

    /** Milliseconds from [now] to the next time the clock shows [minuteOfDay] (always in the future). */
    fun delayUntilNextMillis(now: Long, minuteOfDay: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val minute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        val nowLocal = Instant.ofEpochMilli(now).atZone(zone)
        val time = LocalTime.of(minute / 60, minute % 60)
        var next = nowLocal.toLocalDate().atTime(time).atZone(zone)
        if (!next.isAfter(nowLocal)) next = nowLocal.toLocalDate().plusDays(1).atTime(time).atZone(zone)
        return Duration.between(nowLocal, next).toMillis()
    }

    private fun epochDay(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}