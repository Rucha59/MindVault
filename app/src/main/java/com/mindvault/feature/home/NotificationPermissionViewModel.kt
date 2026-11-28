package com.mindvault.feature.home

import androidx.lifecycle.ViewModel
import com.mindvault.domain.usecase.ObserveNotificationSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val observeSettings: ObserveNotificationSettingsUseCase,
) : ViewModel() {
    /** True when at least one notification kind is switched on, so the permission is worth asking for. */
    suspend fun anyNotificationEnabled(): Boolean =
        observeSettings().first().let { it.dueRemindersEnabled || it.nudgesEnabled }
}