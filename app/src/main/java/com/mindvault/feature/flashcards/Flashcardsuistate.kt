package com.mindvault.feature.flashcards

import com.mindvault.domain.model.DeckSummary

sealed interface DecksUiState {
    data object Loading : DecksUiState
    data object Error : DecksUiState
    data class Content(val decks: List<DeckSummary>) : DecksUiState
}