package com.mindvault.feature.flashcards

import com.mindvault.domain.model.ReviewMode
import com.mindvault.domain.model.ReviewSession

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data object Error : ReviewUiState
    data class Empty(val deckName: String, val mode: ReviewMode) : ReviewUiState

    /** [isSaving] blocks a second tap while a rating is being saved; [message] is a retryable save error. */
    data class Active(
        val deckName: String,
        val mode: ReviewMode,
        val session: ReviewSession,
        val isSaving: Boolean = false,
        val message: String? = null,
    ) : ReviewUiState
}