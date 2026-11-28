package com.mindvault.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.ProgressStats
import com.mindvault.domain.usecase.ObserveProgressStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Stats for the Profile card and the Stats screen. Null until the first numbers arrive. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    observeStats: ObserveProgressStatsUseCase,
) : ViewModel() {
    val stats: StateFlow<ProgressStats?> =
        observeStats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}