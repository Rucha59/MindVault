package com.mindvault.feature.knowledge

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.KNOWLEDGE_ID_ARG
import com.mindvault.domain.model.DeckOption
import com.mindvault.domain.model.SaveDeckResult
import com.mindvault.domain.usecase.AddItemsToDeckUseCase
import com.mindvault.domain.usecase.CreateDeckUseCase
import com.mindvault.domain.usecase.ObserveDeckOptionsUseCase
import com.mindvault.feature.flashcards.DeckFormUiState
import com.mindvault.feature.flashcards.toFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AddToDeckVM"

/**
 * Backs the "Add to flashcards" sheet on the item detail screen. It is created when the sheet is
 * first shown and reads the item ID from the same back-stack entry as the detail screen.
 */
@HiltViewModel
class AddToDeckViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDeckOptions: ObserveDeckOptionsUseCase,
    private val addItemsToDeck: AddItemsToDeckUseCase,
    private val createDeck: CreateDeckUseCase,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle.get<String>(KNOWLEDGE_ID_ARG))
    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<AddToDeckUiState> = combine(
        observeDeckOptions(itemId)
            .map<List<DeckOption>, List<DeckOption>?> { it }
            .catch { e ->
                Log.e(TAG, "Failed to load decks", e)
                emit(null)
            },
        local,
    ) { options, state ->
        if (options == null) {
            AddToDeckUiState(isLoading = false, loadError = true)
        } else {
            // Decks arrive default-first, so the default deck is preselected when it is still available.
            val selectable = options.filterNot { it.containsItem }
            val selected = state.selectedDeckId?.takeIf { id -> selectable.any { it.deck.id == id } }
                ?: selectable.firstOrNull()?.deck?.id
            AddToDeckUiState(
                isLoading = false,
                options = options,
                selectedDeckId = selected,
                isAdding = state.isAdding,
                error = state.error,
                isDone = state.isDone,
                form = state.form,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddToDeckUiState())

    fun onDeckSelected(deckId: String) = local.update { it.copy(selectedDeckId = deckId, error = null) }

    fun onAdd() {
        val deckId = uiState.value.selectedDeckId ?: return
        if (uiState.value.isAdding) return
        local.update { it.copy(isAdding = true, error = null) }
        viewModelScope.launch {
            try {
                addItemsToDeck(deckId, listOf(itemId))
                local.update { it.copy(isAdding = false, isDone = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add the card", e)
                local.update { it.copy(isAdding = false, error = "Couldn't add the card. Please try again.") }
            }
        }
    }

    fun onCreateDeck(name: String) {
        if (local.value.form.isSaving) return
        local.update { it.copy(form = DeckFormUiState(isSaving = true)) }
        viewModelScope.launch {
            try {
                val result = createDeck(name)
                local.update {
                    it.copy(
                        form = result.toFormState(),
                        // Select the new deck as soon as it exists.
                        selectedDeckId = (result as? SaveDeckResult.Saved)?.deck?.id ?: it.selectedDeckId,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create a deck", e)
                local.update { it.copy(form = DeckFormUiState(error = "Couldn't create the deck. Please try again.")) }
            }
        }
    }

    fun onNewDeckFormDismissed() = local.update { it.copy(form = DeckFormUiState()) }

    /** Called when the sheet closes, so the next time it opens it starts fresh. */
    fun onClosed() = local.update { LocalState() }

    private data class LocalState(
        val selectedDeckId: String? = null,
        val isAdding: Boolean = false,
        val error: String? = null,
        val isDone: Boolean = false,
        val form: DeckFormUiState = DeckFormUiState(),
    )
}