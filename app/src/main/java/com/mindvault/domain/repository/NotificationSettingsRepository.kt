package com.mindvault.domain.repository

import com.mindvault.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface NotificationSettingsRepository {
    fun observe(): Flow<NotificationSettings>
    suspend fun get(): NotificationSettings = observe().first()
    suspend fun setDueRemindersEnabled(enabled: Boolean)
    suspend fun setReminderMinuteOfDay(minuteOfDay: Int)
    suspend fun setNudgesEnabled(enabled: Boolean)
    suspend fun recordNudge(epochDay: Long, lineIndex: Int)

    /** Stores [epochDay] as the first-scheduled day unless one is already stored. */
    suspend fun ensureFirstSeen(epochDay: Long)
}