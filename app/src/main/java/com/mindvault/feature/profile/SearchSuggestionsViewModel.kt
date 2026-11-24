package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.usecase.ClearRecentSearchesUseCase
import com.mindvault.domain.usecase.ObserveRecentSearchesUseCase
import com.mindvault.domain.usecase.ObserveSearchSuggestionsEnabledUseCase
import com.mindvault.domain.usecase.SetSearchSuggestionsEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the "Search suggestions" setting. Separate from ProfileViewModel so it can be removed alone. */
@HiltViewModel
class SearchSuggestionsViewModel @Inject constructor(
    observeEnabled: ObserveSearchSuggestionsEnabledUseCase,
    private val setEnabled: SetSearchSuggestionsEnabledUseCase,
    observeRecentSearches: ObserveRecentSearchesUseCase,
    private val clearRecentSearches: ClearRecentSearchesUseCase,
) : ViewModel() {

    val enabled: StateFlow<Boolean> = observeEnabled().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val hasRecentSearches: StateFlow<Boolean> = observeRecentSearches()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun onToggle(value: Boolean) {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                setEnabled(value)
            } catch (e: IOException) {
                _saveFailed.value = true
            }
        }
    }

    fun onClearRecentSearches() {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                clearRecentSearches()
            } catch (e: IOException) {
                _saveFailed.value = true
            }
        }
    }
}