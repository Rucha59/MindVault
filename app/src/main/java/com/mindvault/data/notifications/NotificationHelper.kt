package com.mindvault.data.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mindvault.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the two channels and posts the two kinds of notification. Tapping either opens the app (Home). */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager get() = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_DUE, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_due))
                .setDescription(context.getString(R.string.notification_channel_due_description))
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_NUDGE, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_channel_nudge))
                .setDescription(context.getString(R.string.notification_channel_nudge_description))
                .build(),
        )
    }

    fun nudgeLineCount(): Int = context.resources.getStringArray(R.array.nudge_lines).size

    fun showDueReminder(count: Int) {
        val body = context.resources.getQuantityString(R.plurals.notification_due_body, count, count)
        post(ID_DUE, CHANNEL_DUE, context.getString(R.string.notification_due_title), body, NotificationCompat.CATEGORY_REMINDER)
    }

    fun showNudge(lineIndex: Int) {
        val lines = context.resources.getStringArray(R.array.nudge_lines)
        if (lines.isEmpty()) return
        post(ID_NUDGE, CHANNEL_NUDGE, context.getString(R.string.app_name), lines[lineIndex.mod(lines.size)], null)
    }

    // areNotificationsEnabled() is false when the Android 13+ permission is missing, so nothing is posted without it.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, channel: String, title: String, text: String, category: String?) {
        if (!manager.areNotificationsEnabled()) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val tap = launch?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .apply { if (category != null) setCategory(category) }
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL_DUE = "study_reminders"
        const val CHANNEL_NUDGE = "comeback_nudges"
        const val ID_DUE = 1001
        const val ID_NUDGE = 1002
    }
}