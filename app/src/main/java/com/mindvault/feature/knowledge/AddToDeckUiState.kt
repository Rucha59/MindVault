package com.mindvault.feature.knowledge

import com.mindvault.domain.model.DeckOption
import com.mindvault.feature.flashcards.DeckFormUiState

data class AddToDeckUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val options: List<DeckOption> = emptyList(),
    val selectedDeckId: String? = null,
    val isAdding: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
    val form: DeckFormUiState = DeckFormUiState(),
) {
    val canAdd: Boolean get() = selectedDeckId != null && !isAdding
    val allDecksHaveItem: Boolean get() = options.isNotEmpty() && options.all { it.containsItem }
}