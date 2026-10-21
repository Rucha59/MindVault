package com.mindvault.feature.vault

import com.mindvault.domain.model.VaultEntry

sealed interface VaultUiState {
    data object Loading : VaultUiState
    data object Empty : VaultUiState
    data object Error : VaultUiState
    data class Content(val entries: List<VaultEntry>) : VaultUiState
}