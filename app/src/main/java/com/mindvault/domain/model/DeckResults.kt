package com.mindvault.domain.model

sealed interface SaveDeckResult {
    data class Saved(val deck: Deck) : SaveDeckResult
    data class Invalid(val error: ValidationError) : SaveDeckResult
    data object DuplicateName : SaveDeckResult
    data object NotFound : SaveDeckResult
}

sealed interface DeleteDeckResult {
    data object Deleted : DeleteDeckResult
    data object DefaultProtected : DeleteDeckResult
}

data class AddItemsToDeckResult(
    val added: Int,
    val alreadyInDeck: Int,
)