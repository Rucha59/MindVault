package com.mindvault.feature.vault

import com.mindvault.domain.model.Category
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
)