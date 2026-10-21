package com.mindvault.feature.knowledge

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.KNOWLEDGE_ID_ARG
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.usecase.DeleteKnowledgeItemUseCase
import com.mindvault.domain.usecase.ObserveKnowledgeDetailUseCase
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

private const val TAG = "KnowledgeDetailVM"

@HiltViewModel
class KnowledgeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeKnowledgeDetail: ObserveKnowledgeDetailUseCase,
    private val deleteKnowledgeItem: DeleteKnowledgeItemUseCase,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle.get<String>(KNOWLEDGE_ID_ARG))

    private val _deleteState = MutableStateFlow(DeleteUiState())
    val deleteState: StateFlow<DeleteUiState> = _deleteState.asStateFlow()

    private val content = observeKnowledgeDetail(itemId)
        .map<VaultEntry?, KnowledgeDetailUiState> { entry ->
            if (entry == null) KnowledgeDetailUiState.NotFound else KnowledgeDetailUiState.Content(entry)
        }

    // Once deleted, show Loading instead of a brief "not found" while the screen closes.
    val uiState: StateFlow<KnowledgeDetailUiState> =
        combine<KnowledgeDetailUiState, DeleteUiState, KnowledgeDetailUiState>(content, _deleteState) { state, delete ->
            if (delete.isDeleted) KnowledgeDetailUiState.Loading else state
        }
            .catch { e ->
                Log.e(TAG, "Failed to load item", e)
                emit(KnowledgeDetailUiState.Error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnowledgeDetailUiState.Loading)

    fun onDeleteConfirmed() {
        if (_deleteState.value.isDeleting) return
        _deleteState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            try {
                deleteKnowledgeItem(itemId)
                _deleteState.update { it.copy(isDeleting = false, isDeleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)
                _deleteState.update {
                    it.copy(isDeleting = false, error = "Couldn't delete this item. Please try again.")
                }
            }
        }
    }

    fun onDeleteDialogDismissed() = _deleteState.update { it.copy(error = null) }
}