package com.mindvault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindvault.domain.model.NotificationSettings
import com.mindvault.domain.repository.NotificationSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "mindvault_notifications")

/** Notification preferences in their own DataStore file, so SettingsRepository stays untouched. */
@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationSettingsRepository {

    override fun observe(): Flow<NotificationSettings> =
        context.notificationDataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { p ->
                NotificationSettings(
                    dueRemindersEnabled = p[DUE_ENABLED] ?: true,
                    reminderMinuteOfDay = (p[REMINDER_MINUTE] ?: NotificationSettings.DEFAULT_REMINDER_MINUTE_OF_DAY)
                        .coerceIn(0, MAX_MINUTE),
                    nudgesEnabled = p[NUDGES_ENABLED] ?: true,
                    lastNudgeEpochDay = p[LAST_NUDGE_DAY],
                    lastNudgeLine = p[LAST_NUDGE_LINE] ?: -1,
                    firstSeenEpochDay = p[FIRST_SEEN_DAY],
                )
            }
            .distinctUntilChanged()

    override suspend fun setDueRemindersEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[DUE_ENABLED] = enabled }
    }

    override suspend fun setReminderMinuteOfDay(minuteOfDay: Int) {
        context.notificationDataStore.edit { it[REMINDER_MINUTE] = minuteOfDay.coerceIn(0, MAX_MINUTE) }
    }

    override suspend fun setNudgesEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[NUDGES_ENABLED] = enabled }
    }

    override suspend fun recordNudge(epochDay: Long, lineIndex: Int) {
        context.notificationDataStore.edit {
            it[LAST_NUDGE_DAY] = epochDay
            it[LAST_NUDGE_LINE] = lineIndex
        }
    }

    override suspend fun ensureFirstSeen(epochDay: Long) {
        context.notificationDataStore.edit { if (it[FIRST_SEEN_DAY] == null) it[FIRST_SEEN_DAY] = epochDay }
    }

    private companion object {
        const val MAX_MINUTE = 24 * 60 - 1
        val DUE_ENABLED = booleanPreferencesKey("due_reminders_enabled")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute_of_day")
        val NUDGES_ENABLED = booleanPreferencesKey("nudges_enabled")
        val LAST_NUDGE_DAY = longPreferencesKey("last_nudge_epoch_day")
        val LAST_NUDGE_LINE = intPreferencesKey("last_nudge_line")
        val FIRST_SEEN_DAY = longPreferencesKey("first_seen_epoch_day")
    }
}