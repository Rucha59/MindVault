package com.mindvault.feature.flashcards

import com.mindvault.domain.model.StudySession

sealed interface StudyUiState {
    data object Loading : StudyUiState
    data object Error : StudyUiState
    data class Empty(val deckName: String) : StudyUiState

    /** [message] is a non-blocking warning, for example "couldn't save progress for a card". */
    data class Active(
        val deckName: String,
        val session: StudySession,
        val message: String? = null,
    ) : StudyUiState
}