package com.mindvault.feature.vault

import com.mindvault.domain.model.Category
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.model.VaultQuery

enum class VaultStatus { Loading, Ready, Error }

data class VaultUiState(
    val status: VaultStatus = VaultStatus.Loading,
    val query: VaultQuery = VaultQuery(),
    val categories: List<Category> = emptyList(),
    /** All saved items, before search and filters. Zero means the vault is truly empty. */
    val totalCount: Int = 0,
    val results: List<VaultEntry> = emptyList(),
    /** The flashcard deck of each category that has one, by category ID. */
    val categoryDecks: Map<String, Deck> = emptyMap(),
)

/** Results of the category menu actions: rename, delete and create deck. */
data class CategoryActionsUiState(
    val renameError: String? = null,
    val isRenaming: Boolean = false,
    val renameDone: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val deleteDone: Boolean = false,
    /** A short result such as "Category renamed." or an error from creating the deck. */
    val message: String? = null,
)