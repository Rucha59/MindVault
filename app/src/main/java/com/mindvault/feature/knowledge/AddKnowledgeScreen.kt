package com.mindvault.feature.knowledge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTextField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.domain.model.KnowledgeField
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.usecase.CreateCategoryUseCase
import com.mindvault.domain.usecase.SaveKnowledgeItemUseCase

/** Add form, and Edit when the route carries an item ID (the ViewModel decides). */
@Composable
fun AddKnowledgeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddKnowledgeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnBack by rememberUpdatedState(onBackClick)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddKnowledgeEvent.Saved -> currentOnBack()
            }
        }
    }

    fun requestBack() {
        when {
            state.isSaving -> Unit
            state.isDirty -> showDiscardDialog = true
            else -> onBackClick()
        }
    }

    BackHandler { requestBack() }

    Column(modifier.fillMaxSize().imePadding()) {
        MindVaultTopBar(
            title = if (state.isEditing) "Edit concept" else "Add a concept",
            onBackClick = { requestBack() },
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            state.loadError -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MindVaultSpacing.screenHorizontal),
            ) {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't open this item",
                    message = "It may have been removed. Go back and try again.",
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MindVaultSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space4),
                ) {
                    MindVaultTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = "Title (required)",
                        singleLine = true,
                        errorText = state.fieldErrors[KnowledgeField.TITLE]
                            ?.message("Enter a title", SaveKnowledgeItemUseCase.MAX_TITLE_LENGTH),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    TypeSelector(selected = state.type, onSelected = viewModel::onTypeChange)

                    CategoryPicker(
                        categories = state.categories,
                        selectedCategoryId = state.categoryId,
                        onCategorySelected = viewModel::onCategorySelected,
                        onCreateNewClick = viewModel::onNewCategoryRequested,
                    )

                    MindVaultTextField(
                        value = state.definition,
                        onValueChange = viewModel::onDefinitionChange,
                        label = "Definition (required)",
                        minLines = 4,
                        maxLines = 12,
                        errorText = state.fieldErrors[KnowledgeField.DEFINITION]
                            ?.message("Enter a definition", SaveKnowledgeItemUseCase.MAX_DEFINITION_LENGTH),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )

                    MindVaultTextField(
                        value = state.personalNotes,
                        onValueChange = viewModel::onNotesChange,
                        label = "Personal notes (optional)",
                        minLines = 3,
                        maxLines = 10,
                        errorText = state.fieldErrors[KnowledgeField.PERSONAL_NOTES]
                            ?.message("", SaveKnowledgeItemUseCase.MAX_NOTES_LENGTH),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )

                    MindVaultTextField(
                        value = state.example,
                        onValueChange = viewModel::onExampleChange,
                        label = "Example (optional)",
                        minLines = 2,
                        maxLines = 6,
                        errorText = state.fieldErrors[KnowledgeField.EXAMPLE]
                            ?.message("", SaveKnowledgeItemUseCase.MAX_EXAMPLE_LENGTH),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MindVaultSpacing.screenHorizontal,
                            vertical = MindVaultSpacing.space3,
                        ),
                ) {
                    state.saveError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = MindVaultSpacing.space2),
                        )
                    }
                    PrimaryButton(
                        text = when {
                            state.isSaving -> "Saving…"
                            state.isEditing -> "Save changes"
                            else -> "Save"
                        },
                        onClick = viewModel::onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.showNewCategoryDialog) {
        NewCategoryDialog(
            error = state.newCategoryError,
            onConfirm = viewModel::onNewCategoryConfirmed,
            onDismiss = viewModel::onNewCategoryDismissed,
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = MindVaultColors.Surface,
            title = {
                Text(
                    if (state.isEditing) "Discard your changes?" else "Discard this concept?",
                    color = MindVaultColors.TextPrimary,
                )
            },
            text = {
                Text(
                    if (state.isEditing) "Your changes haven't been saved." else "What you entered hasn't been saved.",
                    color = MindVaultColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Keep editing") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeSelector(
    selected: KnowledgeType,
    onSelected: (KnowledgeType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
        Text(
            text = "Type",
            style = MaterialTheme.typography.labelLarge,
            color = MindVaultColors.TextPrimary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
            KnowledgeType.entries.forEach { type ->
                val isSelected = type == selected
                // A check mark marks the selected chip, so selection isn't shown by color alone.
                val checkIcon: (@Composable () -> Unit)? = if (isSelected) {
                    ({
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    })
                } else null
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(type) },
                    label = { Text(type.label()) },
                    leadingIcon = checkIcon,
                    shape = ShapePill,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MindVaultColors.SurfaceVariant,
                        labelColor = MindVaultColors.TextPrimary,
                        selectedContainerColor = MindVaultColors.Forest,
                        selectedLabelColor = MindVaultColors.TextOnPrimary,
                        selectedLeadingIconColor = MindVaultColors.TextOnPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(
    error: ValidationError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MindVaultColors.Surface,
        title = { Text("New category", color = MindVaultColors.TextPrimary) },
        text = {
            MindVaultTextField(
                value = name,
                onValueChange = { name = it },
                label = "Category name",
                singleLine = true,
                errorText = error?.message("Enter a category name", CreateCategoryUseCase.MAX_NAME_LENGTH),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Cancel") }
        },
    )
}

private fun ValidationError.message(blank: String, max: Int): String = when (this) {
    ValidationError.BLANK -> blank
    ValidationError.TOO_LONG -> "Keep this under $max characters"
}