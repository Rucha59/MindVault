package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.NotificationSettings
import com.mindvault.domain.usecase.ObserveNotificationSettingsUseCase
import com.mindvault.domain.usecase.UpdateNotificationSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the "Notifications" setting. Separate from ProfileViewModel so it can be removed alone. */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    observeSettings: ObserveNotificationSettingsUseCase,
    private val update: UpdateNotificationSettingsUseCase,
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = observeSettings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotificationSettings(),
    )

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun onDueRemindersChanged(enabled: Boolean) = save { update.setDueReminders(enabled) }
    fun onReminderTimeChanged(minuteOfDay: Int) = save { update.setReminderTime(minuteOfDay) }
    fun onNudgesChanged(enabled: Boolean) = save { update.setNudges(enabled) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                block()
            } catch (e: IOException) {
                _saveFailed.value = true
            }
        }
    }
}