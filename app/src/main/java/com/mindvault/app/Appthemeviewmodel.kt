package com.mindvault.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.usecase.ObserveThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Activity-level holder of the saved theme preference, read by MainActivity to drive MindVaultTheme. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeThemePreference: ObserveThemePreferenceUseCase,
) : ViewModel() {
    val themePreference: StateFlow<ThemePreference> = observeThemePreference()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)
}