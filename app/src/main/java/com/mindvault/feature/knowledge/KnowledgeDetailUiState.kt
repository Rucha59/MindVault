package com.mindvault.feature.knowledge

import com.mindvault.domain.model.VaultEntry

sealed interface KnowledgeDetailUiState {
    data object Loading : KnowledgeDetailUiState
    data object NotFound : KnowledgeDetailUiState
    data object Error : KnowledgeDetailUiState
    data class Content(val entry: VaultEntry) : KnowledgeDetailUiState
}

data class DeleteUiState(
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
)