package com.mindvault.domain.model

sealed interface AddToDeckResult {
    data class Added(val flashcard: Flashcard) : AddToDeckResult
    data object AlreadyInDeck : AddToDeckResult
}