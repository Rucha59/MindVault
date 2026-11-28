package com.mindvault.domain.model

/** Notification preferences (per device, not synced). Both kinds are on by default. */
data class NotificationSettings(
    val dueRemindersEnabled: Boolean = true,
    val reminderMinuteOfDay: Int = DEFAULT_REMINDER_MINUTE_OF_DAY,
    val nudgesEnabled: Boolean = true,
    val lastNudgeEpochDay: Long? = null,
    val lastNudgeLine: Int = -1,
    /** Day notifications were first scheduled; the starting point for the come-back nudge of a user who never saved. */
    val firstSeenEpochDay: Long? = null,
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTE_OF_DAY = 9 * 60
    }
}

/** What the planner decided to send. */
sealed interface NotificationPlan {
    data object None : NotificationPlan
    data class DueReminder(val count: Int) : NotificationPlan
    data class Nudge(val lineIndex: Int) : NotificationPlan
}