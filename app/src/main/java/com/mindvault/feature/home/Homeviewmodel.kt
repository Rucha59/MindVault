package com.mindvault.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.HomeSummary
import com.mindvault.domain.usecase.ObserveHomeSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeSummary: ObserveHomeSummaryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeHomeSummary()
        .map<HomeSummary, HomeUiState> { HomeUiState.Content(it) }
        .catch { emit(HomeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}