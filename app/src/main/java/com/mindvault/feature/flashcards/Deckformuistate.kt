package com.mindvault.feature.flashcards

import com.mindvault.domain.model.SaveDeckResult
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.usecase.DeckNameValidator

/** State of a create/rename deck dialog. */
data class DeckFormUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
)

internal fun SaveDeckResult.toFormState(): DeckFormUiState = when (this) {
    is SaveDeckResult.Saved -> DeckFormUiState(isDone = true)
    is SaveDeckResult.Invalid -> DeckFormUiState(
        error = when (error) {
            ValidationError.BLANK -> "Enter a deck name"
            ValidationError.TOO_LONG -> "Keep this under ${DeckNameValidator.MAX_NAME_LENGTH} characters"
        },
    )
    SaveDeckResult.DuplicateName -> DeckFormUiState(error = "A deck with this name already exists")
    SaveDeckResult.NotFound -> DeckFormUiState(error = "This deck no longer exists")
}