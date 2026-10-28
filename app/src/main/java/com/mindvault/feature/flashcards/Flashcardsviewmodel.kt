package com.mindvault.feature.flashcards

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.DeckSummary
import com.mindvault.domain.usecase.CreateDeckUseCase
import com.mindvault.domain.usecase.ObserveDeckSummariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "FlashcardsVM"

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    observeDeckSummaries: ObserveDeckSummariesUseCase,
    private val createDeck: CreateDeckUseCase,
) : ViewModel() {

    val uiState: StateFlow<DecksUiState> = observeDeckSummaries()
        .map<List<DeckSummary>, DecksUiState> { DecksUiState.Content(it) }
        .catch { e ->
            Log.e(TAG, "Failed to load decks", e)
            emit(DecksUiState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading)

    private val _deckForm = MutableStateFlow(DeckFormUiState())
    val deckForm: StateFlow<DeckFormUiState> = _deckForm.asStateFlow()

    fun onCreateDeck(name: String) {
        if (_deckForm.value.isSaving) return
        _deckForm.value = DeckFormUiState(isSaving = true)
        viewModelScope.launch {
            try {
                _deckForm.value = createDeck(name).toFormState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create deck", e)
                _deckForm.value = DeckFormUiState(error = "Couldn't create the deck. Please try again.")
            }
        }
    }

    fun onDeckFormDismissed() {
        _deckForm.value = DeckFormUiState()
    }
}