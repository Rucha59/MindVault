package com.mindvault.feature.vault

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.CategoryFilter
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.VaultQuery
import com.mindvault.domain.model.VaultSort
import com.mindvault.domain.usecase.ApplyVaultQueryUseCase
import com.mindvault.domain.usecase.ObserveCategoriesUseCase
import com.mindvault.domain.usecase.ObserveVaultItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class VaultViewModel @Inject constructor(
    observeVaultItems: ObserveVaultItemsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val applyVaultQuery: ApplyVaultQueryUseCase,
) : ViewModel() {

    /**
     * Plain Compose state rather than a StateFlow: a text field bound to an asynchronously
     * delivered value can drop characters or jump the cursor while typing.
     */
    var searchText by mutableStateOf("")
        private set

    private val filters = MutableStateFlow(VaultQuery())

    val uiState: StateFlow<VaultUiState> = combine(
        observeVaultItems(),
        observeCategories(),
        snapshotFlow { searchText },
        filters,
    ) { entries, categories, text, filter ->
        val query = filter.copy(searchText = text)
        VaultUiState(
            status = VaultStatus.Ready,
            query = query,
            categories = categories,
            totalCount = entries.size,
            results = applyVaultQuery(entries, query),
        )
    }
        .catch { e ->
            Log.e("VaultViewModel", "Failed to load vault", e)
            emit(VaultUiState(status = VaultStatus.Error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    fun onSearchChange(text: String) {
        searchText = text
    }

    fun onTypeFilterChange(type: KnowledgeType?) = filters.update { it.copy(type = type) }

    fun onCategoryFilterChange(category: CategoryFilter) = filters.update { it.copy(category = category) }

    fun onSortChange(sort: VaultSort) = filters.update { it.copy(sort = sort) }

    /** Clears search, type and category. The chosen sort order is kept. */
    fun onClearFilters() {
        searchText = ""
        filters.update { it.copy(type = null, category = CategoryFilter.All) }
    }
}