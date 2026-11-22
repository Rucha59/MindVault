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
import com.mindvault.domain.model.RenameCategoryResult
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.model.VaultQuery
import com.mindvault.domain.model.VaultSort
import com.mindvault.domain.usecase.ApplyVaultQueryUseCase
import com.mindvault.domain.usecase.CreateCategoryDeckUseCase
import com.mindvault.domain.usecase.CreateCategoryUseCase
import com.mindvault.domain.usecase.DeleteCategoryUseCase
import com.mindvault.domain.usecase.ObserveCategoriesUseCase
import com.mindvault.domain.usecase.ObserveCategoryDecksUseCase
import com.mindvault.domain.usecase.ObserveVaultItemsUseCase
import com.mindvault.domain.usecase.RenameCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "VaultViewModel"

@HiltViewModel
class VaultViewModel @Inject constructor(
    observeVaultItems: ObserveVaultItemsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeCategoryDecks: ObserveCategoryDecksUseCase,
    private val applyVaultQuery: ApplyVaultQueryUseCase,
    private val renameCategory: RenameCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val createCategoryDeck: CreateCategoryDeckUseCase,
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
        observeCategoryDecks(),
        snapshotFlow { searchText },
        filters,
    ) { entries, categories, categoryDecks, text, filter ->
        val query = filter.copy(searchText = text)
        VaultUiState(
            status = VaultStatus.Ready,
            query = query,
            categories = categories,
            totalCount = entries.size,
            results = applyVaultQuery(entries, query),
            categoryDecks = categoryDecks,
        )
    }
        .catch { e ->
            Log.e(TAG, "Failed to load vault", e)
            emit(VaultUiState(status = VaultStatus.Error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    private val _actions = MutableStateFlow(CategoryActionsUiState())
    val actions: StateFlow<CategoryActionsUiState> = _actions.asStateFlow()

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

    // ---------------------------------------------------------------- category menu

    fun onRenameCategory(categoryId: String, name: String) {
        if (_actions.value.isRenaming) return
        _actions.update { it.copy(isRenaming = true, renameError = null, message = null) }
        viewModelScope.launch {
            try {
                when (val result = renameCategory(categoryId, name)) {
                    is RenameCategoryResult.Renamed -> _actions.update {
                        it.copy(isRenaming = false, renameDone = true, message = "Category renamed.")
                    }
                    is RenameCategoryResult.Invalid -> _actions.update {
                        it.copy(
                            isRenaming = false,
                            renameError = when (result.error) {
                                ValidationError.BLANK -> "Enter a category name"
                                ValidationError.TOO_LONG ->
                                    "Keep this under ${CreateCategoryUseCase.MAX_NAME_LENGTH} characters"
                            },
                        )
                    }
                    RenameCategoryResult.DuplicateName -> _actions.update {
                        it.copy(isRenaming = false, renameError = "A category with this name already exists")
                    }
                    RenameCategoryResult.NotFound -> _actions.update {
                        it.copy(isRenaming = false, renameError = "This category no longer exists")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename category", e)
                _actions.update {
                    it.copy(isRenaming = false, renameError = "Couldn't rename the category. Please try again.")
                }
            }
        }
    }

    fun onRenameDismissed() = _actions.update { it.copy(renameError = null, renameDone = false) }

    fun onDeleteCategory(categoryId: String, deleteDeck: Boolean) {
        if (_actions.value.isDeleting) return
        _actions.update { it.copy(isDeleting = true, deleteError = null, message = null) }
        viewModelScope.launch {
            try {
                deleteCategory(categoryId, deleteDeck)
                // The deleted category can no longer be the active filter.
                filters.update { it.copy(category = CategoryFilter.All) }
                _actions.update { it.copy(isDeleting = false, deleteDone = true, message = "Category deleted.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete category", e)
                _actions.update {
                    it.copy(isDeleting = false, deleteError = "Couldn't delete the category. Please try again.")
                }
            }
        }
    }

    fun onDeleteDismissed() = _actions.update { it.copy(deleteError = null, deleteDone = false) }

    fun onCreateCategoryDeck(categoryId: String) {
        _actions.update { it.copy(message = null) }
        viewModelScope.launch {
            try {
                val deck = createCategoryDeck(categoryId)
                _actions.update {
                    it.copy(
                        message = if (deck == null) {
                            "This category no longer exists."
                        } else {
                            "Created the deck “${deck.name}”. You'll find it under Flashcards."
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create category deck", e)
                _actions.update { it.copy(message = "Couldn't create the deck. Please try again.") }
            }
        }
    }

    fun onMessageDismissed() = _actions.update { it.copy(message = null) }
}