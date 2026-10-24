package com.mindvault.feature.knowledge

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.KNOWLEDGE_ID_ARG
import com.mindvault.core.navigation.PREFILL_DEFINITION_ARG
import com.mindvault.core.navigation.PREFILL_EXAMPLE_ARG
import com.mindvault.core.navigation.PREFILL_TITLE_ARG
import com.mindvault.domain.model.CreateCategoryResult
import com.mindvault.domain.model.KnowledgeField
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SaveKnowledgeResult
import com.mindvault.domain.model.UpdateKnowledgeResult
import com.mindvault.domain.usecase.CreateCategoryUseCase
import com.mindvault.domain.usecase.ObserveCategoriesUseCase
import com.mindvault.domain.usecase.ObserveKnowledgeDetailUseCase
import com.mindvault.domain.usecase.SaveKnowledgeItemUseCase
import com.mindvault.domain.usecase.UpdateKnowledgeItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AddKnowledgeVM"

/**
 * Serves Add (no item ID in the route), Edit (item ID present) and Add-from-Search
 * (prefill arguments present; the form starts filled in and counts as unsaved).
 */
@HiltViewModel
class AddKnowledgeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCategories: ObserveCategoriesUseCase,
    private val observeKnowledgeDetail: ObserveKnowledgeDetailUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val saveKnowledgeItem: SaveKnowledgeItemUseCase,
    private val updateKnowledgeItem: UpdateKnowledgeItemUseCase,
) : ViewModel() {

    private val editingItemId: String? = savedStateHandle.get<String>(KNOWLEDGE_ID_ARG)
    private var originalItem: KnowledgeItem? = null

    /** Set only when the form was opened from a Search result with "Edit first". */
    private val prefill: KnowledgeFormValues? = savedStateHandle.get<String>(PREFILL_TITLE_ARG)?.let { title ->
        KnowledgeFormValues(
            title = title,
            definition = savedStateHandle.get<String>(PREFILL_DEFINITION_ARG).orEmpty(),
            example = savedStateHandle.get<String>(PREFILL_EXAMPLE_ARG).orEmpty(),
        )
    }

    private val _uiState = MutableStateFlow(
        AddKnowledgeUiState(
            title = prefill?.title.orEmpty(),
            definition = prefill?.definition.orEmpty(),
            example = prefill?.example.orEmpty(),
            isEditing = editingItemId != null,
            isLoading = editingItemId != null,
        ),
    )
    val uiState: StateFlow<AddKnowledgeUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddKnowledgeEvent>(Channel.BUFFERED)
    val events: Flow<AddKnowledgeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCategories()
                .catch { Log.e(TAG, "Failed to load categories", it) }
                .collect { categories -> _uiState.update { it.copy(categories = categories) } }
        }
        editingItemId?.let { loadItem(it) }
    }

    private fun loadItem(itemId: String) {
        viewModelScope.launch {
            try {
                val item = observeKnowledgeDetail(itemId).first()?.item
                if (item == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
                    return@launch
                }
                originalItem = item
                val values = KnowledgeFormValues(
                    title = item.title,
                    type = item.type,
                    definition = item.definition,
                    personalNotes = item.personalNotes,
                    example = item.example.orEmpty(),
                    categoryId = item.categoryId,
                )
                _uiState.update {
                    it.copy(
                        title = values.title,
                        type = values.type,
                        definition = values.definition,
                        personalNotes = values.personalNotes,
                        example = values.example,
                        categoryId = values.categoryId,
                        initialValues = values,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load item for editing", e)
                _uiState.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update {
        it.copy(title = value, fieldErrors = it.fieldErrors - KnowledgeField.TITLE, saveError = null)
    }

    fun onTypeChange(value: KnowledgeType) = _uiState.update { it.copy(type = value) }

    fun onDefinitionChange(value: String) = _uiState.update {
        it.copy(definition = value, fieldErrors = it.fieldErrors - KnowledgeField.DEFINITION, saveError = null)
    }

    fun onNotesChange(value: String) = _uiState.update {
        it.copy(personalNotes = value, fieldErrors = it.fieldErrors - KnowledgeField.PERSONAL_NOTES)
    }

    fun onExampleChange(value: String) = _uiState.update {
        it.copy(example = value, fieldErrors = it.fieldErrors - KnowledgeField.EXAMPLE)
    }

    fun onCategorySelected(categoryId: String?) = _uiState.update { it.copy(categoryId = categoryId) }

    fun onNewCategoryRequested() = _uiState.update {
        it.copy(showNewCategoryDialog = true, newCategoryError = null)
    }

    fun onNewCategoryDismissed() = _uiState.update {
        it.copy(showNewCategoryDialog = false, newCategoryError = null)
    }

    fun onNewCategoryConfirmed(name: String) {
        viewModelScope.launch {
            try {
                when (val result = createCategory(name)) {
                    is CreateCategoryResult.Created -> _uiState.update {
                        it.copy(
                            categories = (it.categories + result.category).distinctBy { c -> c.id },
                            categoryId = result.category.id,
                            showNewCategoryDialog = false,
                            newCategoryError = null,
                        )
                    }
                    is CreateCategoryResult.Invalid -> _uiState.update {
                        it.copy(newCategoryError = result.error)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create category", e)
                _uiState.update {
                    it.copy(
                        showNewCategoryDialog = false,
                        saveError = "Couldn't create the category. Please try again.",
                    )
                }
            }
        }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading || current.loadError) return
        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                val original = originalItem
                if (editingItemId != null && original != null) {
                    val edited = original.copy(
                        title = current.title,
                        type = current.type,
                        definition = current.definition,
                        personalNotes = current.personalNotes,
                        example = current.example.ifBlank { null },
                        categoryId = current.categoryId,
                    )
                    when (val result = updateKnowledgeItem(edited)) {
                        // Stay in the saving state until the screen closes, so a second tap can't double-save.
                        is UpdateKnowledgeResult.Updated -> _events.send(AddKnowledgeEvent.Saved)
                        is UpdateKnowledgeResult.Invalid -> _uiState.update {
                            it.copy(isSaving = false, fieldErrors = result.errors)
                        }
                        UpdateKnowledgeResult.NotFound -> _uiState.update {
                            it.copy(isSaving = false, saveError = "This item no longer exists.")
                        }
                    }
                } else {
                    val draft = KnowledgeItemDraft(
                        title = current.title,
                        type = current.type,
                        definition = current.definition,
                        personalNotes = current.personalNotes,
                        example = current.example.ifBlank { null },
                        categoryId = current.categoryId,
                    )
                    when (val result = saveKnowledgeItem(draft)) {
                        is SaveKnowledgeResult.Success -> _events.send(AddKnowledgeEvent.Saved)
                        is SaveKnowledgeResult.Invalid -> _uiState.update {
                            it.copy(isSaving = false, fieldErrors = result.errors)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save knowledge item", e)
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Couldn't save. Your entry is still here, so try again.")
                }
            }
        }
    }
}