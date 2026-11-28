package com.mindvault.feature.profile

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.SectionHeader
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The "Notifications" block of Profile: study reminder (with its time) and come-back nudges.
 * Self-contained: to hide it, delete or comment out the single `NotificationSetting()` call in ProfileScreen.
 * The saved values and the defaults (both on, 9:00) keep working without it.
 */
@Composable
fun NotificationSetting(
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = notificationsAllowed(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notificationsAllowed(context)
    }
    fun askIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !allowed) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SectionHeader("Notifications")
    MindVaultCard(modifier = modifier.fillMaxWidth()) {
        SwitchRow(
            title = "Study reminder",
            supporting = "A daily nudge when cards are due.",
            checked = settings.dueRemindersEnabled,
            onCheckedChange = {
                viewModel.onDueRemindersChanged(it)
                if (it) askIfNeeded()
            },
        )
        if (settings.dueRemindersEnabled) {
            val minute = settings.reminderMinuteOfDay
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Reminder time", style = MaterialTheme.typography.bodyLarge, color = MindVaultColors.TextPrimary)
                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, min -> viewModel.onReminderTimeChanged(hour * 60 + min) },
                            minute / 60,
                            minute % 60,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text(formatTime(minute), style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(MindVaultSpacing.space2))
        SwitchRow(
            title = "Come-back nudges",
            supporting = "An occasional reminder to add what you've learned.",
            checked = settings.nudgesEnabled,
            onCheckedChange = {
                viewModel.onNudgesChanged(it)
                if (it) askIfNeeded()
            },
        )
        if (!allowed && (settings.dueRemindersEnabled || settings.nudgesEnabled)) {
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Text(
                text = "Notifications are turned off for MindVault on this device, so nothing will be sent.",
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextSecondary,
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Open settings") }
        }
    }
    if (saveFailed) {
        Text(
            text = "Couldn't save your choice. Please try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = MindVaultSpacing.space3),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MindVaultColors.TextPrimary)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
        }
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Switch(
            checked = checked,
            onCheckedChange = null, // the whole row handles the click
            colors = SwitchDefaults.colors(
                checkedThumbColor = MindVaultColors.TextOnPrimary,
                checkedTrackColor = MindVaultColors.Forest,
            ),
        )
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun formatTime(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))