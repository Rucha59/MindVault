package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.usecase.ObserveThemePreferenceUseCase
import com.mindvault.domain.usecase.SetThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeThemePreference: ObserveThemePreferenceUseCase,
    private val setThemePreference: SetThemePreferenceUseCase,
) : ViewModel() {

    private val saveFailed = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        observeThemePreference(),
        saveFailed,
    ) { preference, failed -> ProfileUiState(preference, failed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onThemeSelected(preference: ThemePreference) {
        viewModelScope.launch {
            saveFailed.value = false
            try {
                setThemePreference(preference)
            } catch (e: IOException) {
                saveFailed.value = true
            }
        }
    }
}