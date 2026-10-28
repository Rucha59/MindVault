package com.mindvault.feature.home

import com.mindvault.domain.model.HomeSummary

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState
    data class Content(val summary: HomeSummary) : HomeUiState
}