package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.usecase.ObserveNewCardsPerDayUseCase
import com.mindvault.domain.usecase.ReviewQueueBuilder
import com.mindvault.domain.usecase.SetNewCardsPerDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the "New cards per deck per day" setting. Separate from ProfileViewModel so it can be removed alone. */
@HiltViewModel
class NewCardsPerDayViewModel @Inject constructor(
    observeNewCardsPerDay: ObserveNewCardsPerDayUseCase,
    private val setNewCardsPerDay: SetNewCardsPerDayUseCase,
) : ViewModel() {

    val limit: StateFlow<Int> = observeNewCardsPerDay().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReviewQueueBuilder.DEFAULT_NEW_CARDS_PER_DAY,
    )

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun onSelected(value: Int) {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                setNewCardsPerDay(value)
            } catch (e: IOException) {
                _saveFailed.value = true
            }
        }
    }
}