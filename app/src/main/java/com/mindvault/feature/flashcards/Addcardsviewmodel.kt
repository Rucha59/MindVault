package com.mindvault.feature.flashcards

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.DECK_ID_ARG
import com.mindvault.domain.model.VaultQuery
import com.mindvault.domain.model.VaultSort
import com.mindvault.domain.usecase.AddItemsToDeckUseCase
import com.mindvault.domain.usecase.ApplyVaultQueryUseCase
import com.mindvault.domain.usecase.ObserveDeckDetailUseCase
import com.mindvault.domain.usecase.ObserveVaultItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AddCardsVM"

/** Pick Vault items to turn into cards in one deck. Reuses the Vault's own search. */
@HiltViewModel
class AddCardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeVaultItems: ObserveVaultItemsUseCase,
    observeDeckDetail: ObserveDeckDetailUseCase,
    private val applyVaultQuery: ApplyVaultQueryUseCase,
    private val addItemsToDeck: AddItemsToDeckUseCase,
) : ViewModel() {

    private val deckId: String = checkNotNull(savedStateHandle.get<String>(DECK_ID_ARG))

    /** Compose state (not a StateFlow) so typing doesn't lag, as in the Vault search box. */
    var query by mutableStateOf("")
        private set

    private val selection = MutableStateFlow<Set<String>>(emptySet())
    private val operation = MutableStateFlow(Operation())

    val uiState: StateFlow<AddCardsUiState> = combine(
        observeVaultItems(),
        observeDeckDetail(deckId),
        snapshotFlow { query },
        selection,
        operation,
    ) { items, detail, searchText, selected, op ->
        if (detail == null) {
            AddCardsUiState(status = AddCardsStatus.NotFound)
        } else {
            val inDeck = detail.cards.map { it.knowledgeItem.id }.toSet()
            // A_TO_Z gives a stable, predictable order while picking. Assumes VaultQuery's other
            // fields have defaults; if it doesn't compile, pass the "no filter" values here.
            val filtered = applyVaultQuery(items, VaultQuery(searchText = searchText, sort = VaultSort.A_TO_Z))
            AddCardsUiState(
                status = AddCardsStatus.Ready,
                deckName = detail.deck.name,
                totalSaved = items.size,
                entries = filtered.map { PickerEntry(it, it.item.id in inDeck) },
                selectedIds = selected - inDeck,
                isAdding = op.isAdding,
                error = op.error,
                isDone = op.isDone,
            )
        }
    }
        .catch { e ->
            Log.e(TAG, "Failed to load the card picker", e)
            emit(AddCardsUiState(status = AddCardsStatus.Error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddCardsUiState())

    fun onQueryChange(value: String) {
        query = value
    }

    fun onToggle(itemId: String) = selection.update { if (itemId in it) it - itemId else it + itemId }

    fun onAdd() {
        val state = uiState.value
        if (state.isAdding || state.selectedIds.isEmpty()) return
        operation.update { it.copy(isAdding = true, error = null) }
        viewModelScope.launch {
            try {
                addItemsToDeck(deckId, state.selectedIds)
                operation.update { it.copy(isAdding = false, isDone = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add cards", e)
                operation.update {
                    it.copy(isAdding = false, error = "Couldn't add the cards. Your selection is still here, so try again.")
                }
            }
        }
    }

    private data class Operation(
        val isAdding: Boolean = false,
        val error: String? = null,
        val isDone: Boolean = false,
    )
}