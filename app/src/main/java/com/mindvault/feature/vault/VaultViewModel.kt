package com.mindvault.feature.vault

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.usecase.ObserveVaultItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class VaultViewModel @Inject constructor(
    observeVaultItems: ObserveVaultItemsUseCase,
) : ViewModel() {

    val uiState: StateFlow<VaultUiState> = observeVaultItems()
        .map<List<VaultEntry>, VaultUiState> { entries ->
            if (entries.isEmpty()) VaultUiState.Empty else VaultUiState.Content(entries)
        }
        .catch { e ->
            Log.e("VaultViewModel", "Failed to load vault", e)
            emit(VaultUiState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState.Loading)
}