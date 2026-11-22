package com.mindvault.feature.knowledge

import com.mindvault.domain.model.Category
import com.mindvault.domain.model.KnowledgeField
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ValidationError

/** The editable fields, compared against their starting values to detect unsaved changes. */
data class KnowledgeFormValues(
    val title: String = "",
    val type: KnowledgeType = KnowledgeType.WORD,
    val definition: String = "",
    val personalNotes: String = "",
    val example: String = "",
    val categoryId: String? = null,
)

/** Asked when an item leaves a category whose deck holds its card. */
data class DeckRemovalPrompt(
    val itemTitle: String,
    val deckName: String,
)

data class AddKnowledgeUiState(
    val title: String = "",
    val type: KnowledgeType = KnowledgeType.WORD,
    val definition: String = "",
    val personalNotes: String = "",
    val example: String = "",
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val fieldErrors: Map<KnowledgeField, ValidationError> = emptyMap(),
    val showNewCategoryDialog: Boolean = false,
    val newCategoryError: ValidationError? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val initialValues: KnowledgeFormValues = KnowledgeFormValues(),
    /** Non-null while the "remove its card from the old category's deck?" question is showing. */
    val deckRemovalPrompt: DeckRemovalPrompt? = null,
) {
    val currentValues: KnowledgeFormValues
        get() = KnowledgeFormValues(title, type, definition, personalNotes, example, categoryId)

    /** True when the form differs from where it started (empty for Add, the saved item for Edit). */
    val isDirty: Boolean get() = currentValues != initialValues
}

sealed interface AddKnowledgeEvent {
    data object Saved : AddKnowledgeEvent
}