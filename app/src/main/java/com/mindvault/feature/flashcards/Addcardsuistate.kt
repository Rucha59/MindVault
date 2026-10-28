package com.mindvault.feature.flashcards

import com.mindvault.domain.model.VaultEntry

enum class AddCardsStatus { Loading, Ready, NotFound, Error }

/** A Vault item in the picker; [inDeck] items already have a card and can't be picked again. */
data class PickerEntry(
    val entry: VaultEntry,
    val inDeck: Boolean,
)

data class AddCardsUiState(
    val status: AddCardsStatus = AddCardsStatus.Loading,
    val deckName: String = "",
    /** All saved items, before the search filter (tells "Vault empty" from "no matches"). */
    val totalSaved: Int = 0,
    val entries: List<PickerEntry> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isAdding: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
)