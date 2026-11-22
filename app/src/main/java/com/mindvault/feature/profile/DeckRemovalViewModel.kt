package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.DeckRemovalPreference
import com.mindvault.domain.usecase.ObserveDeckRemovalPreferenceUseCase
import com.mindvault.domain.usecase.SetDeckRemovalPreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the "When I take an item out of a category" setting. Separate from ProfileViewModel so it can be removed alone. */
@HiltViewModel
class DeckRemovalViewModel @Inject constructor(
    observePreference: ObserveDeckRemovalPreferenceUseCase,
    private val setPreference: SetDeckRemovalPreferenceUseCase,
) : ViewModel() {

    val preference: StateFlow<DeckRemovalPreference> = observePreference().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DeckRemovalPreference.ASK,
    )

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun onSelected(value: DeckRemovalPreference) {
        viewModelScope.launch {
            _saveFailed.value = false
            try {
                setPreference(value)
            } catch (e: IOException) {
                _saveFailed.value = true
            }
        }
    }
}