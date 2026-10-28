package com.mindvault.feature.flashcards

import com.mindvault.domain.model.DeckDetail

sealed interface DeckDetailUiState {
    data object Loading : DeckDetailUiState
    data object NotFound : DeckDetailUiState
    data object Error : DeckDetailUiState
    data class Content(val detail: DeckDetail) : DeckDetailUiState
}

/** Results of actions on the deck screen: rename form, delete and card removal. */
data class DeckActionsUiState(
    val form: DeckFormUiState = DeckFormUiState(),
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: String? = null,
    val message: String? = null,
)