package com.mindvault.feature.flashcards

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.DECK_ID_ARG
import com.mindvault.domain.model.DeckDetail
import com.mindvault.domain.model.DeleteDeckResult
import com.mindvault.domain.usecase.DeleteDeckUseCase
import com.mindvault.domain.usecase.ObserveDeckDetailUseCase
import com.mindvault.domain.usecase.RemoveCardFromDeckUseCase
import com.mindvault.domain.usecase.RenameDeckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DeckDetailVM"

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDeckDetail: ObserveDeckDetailUseCase,
    private val renameDeck: RenameDeckUseCase,
    private val deleteDeck: DeleteDeckUseCase,
    private val removeCardFromDeck: RemoveCardFromDeckUseCase,
) : ViewModel() {

    private val deckId: String = checkNotNull(savedStateHandle.get<String>(DECK_ID_ARG))

    private val _actions = MutableStateFlow(DeckActionsUiState())
    val actions: StateFlow<DeckActionsUiState> = _actions.asStateFlow()

    private val content = observeDeckDetail(deckId)
        .map<DeckDetail?, DeckDetailUiState> { detail ->
            if (detail == null) DeckDetailUiState.NotFound else DeckDetailUiState.Content(detail)
        }

    // Once deleted, show Loading instead of a brief "not found" while the screen closes.
    val uiState: StateFlow<DeckDetailUiState> =
        combine<DeckDetailUiState, DeckActionsUiState, DeckDetailUiState>(content, _actions) { state, actions ->
            if (actions.isDeleted) DeckDetailUiState.Loading else state
        }
            .catch { e ->
                Log.e(TAG, "Failed to load deck", e)
                emit(DeckDetailUiState.Error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckDetailUiState.Loading)

    fun onRename(name: String) {
        if (_actions.value.form.isSaving) return
        _actions.update { it.copy(form = DeckFormUiState(isSaving = true)) }
        viewModelScope.launch {
            try {
                val result = renameDeck(deckId, name)
                _actions.update { it.copy(form = result.toFormState()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename deck", e)
                _actions.update {
                    it.copy(form = DeckFormUiState(error = "Couldn't rename the deck. Please try again."))
                }
            }
        }
    }

    fun onRenameDismissed() = _actions.update { it.copy(form = DeckFormUiState()) }

    fun onDeleteConfirmed() {
        if (_actions.value.isDeleting) return
        _actions.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            try {
                when (deleteDeck(deckId)) {
                    DeleteDeckResult.Deleted -> _actions.update { it.copy(isDeleting = false, isDeleted = true) }
                    DeleteDeckResult.DefaultProtected -> _actions.update {
                        it.copy(isDeleting = false, deleteError = "The default deck can't be deleted.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete deck", e)
                _actions.update {
                    it.copy(isDeleting = false, deleteError = "Couldn't delete this deck. Please try again.")
                }
            }
        }
    }

    fun onDeleteDismissed() = _actions.update { it.copy(deleteError = null) }

    fun onRemoveCard(flashcardId: String) {
        _actions.update { it.copy(message = null) }
        viewModelScope.launch {
            try {
                removeCardFromDeck(flashcardId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove card", e)
                _actions.update { it.copy(message = "Couldn't remove the card. Please try again.") }
            }
        }
    }
}