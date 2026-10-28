package com.mindvault.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeExtraLarge
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.domain.model.DeckOption
import com.mindvault.feature.flashcards.DeckNameDialog

/**
 * Choose a deck for this Vault item. The default deck is preselected; decks that already hold the
 * item are shown as added and can't be picked. Closes only after the card is saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToDeckSheet(
    onDismiss: () -> Unit,
    viewModel: AddToDeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var showNewDeckDialog by rememberSaveable { mutableStateOf(false) }

    fun close() {
        viewModel.onClosed()
        currentOnDismiss()
    }

    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            viewModel.onClosed()
            currentOnDismiss()
        }
    }
    LaunchedEffect(state.form.isDone) {
        if (state.form.isDone) {
            showNewDeckDialog = false
            viewModel.onNewDeckFormDismissed()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { close() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ShapeExtraLarge,
        containerColor = MindVaultColors.Surface,
        scrimColor = MindVaultColors.Scrim,
    ) {
        Text(
            text = "Add to flashcards",
            style = MaterialTheme.typography.titleLarge,
            color = MindVaultColors.TextPrimary,
            modifier = Modifier
                .padding(horizontal = MindVaultSpacing.screenHorizontal)
                .semantics { heading() },
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(MindVaultSpacing.space6),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            state.loadError -> Text(
                text = "Couldn't load your decks. Close this and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = MindVaultSpacing.screenHorizontal),
            )

            else -> {
                state.options.forEach { option ->
                    DeckOptionRow(
                        option = option,
                        selected = option.deck.id == state.selectedDeckId,
                        onClick = { viewModel.onDeckSelected(option.deck.id) },
                    )
                }
                if (state.allDecksHaveItem) {
                    Text(
                        text = "This item is already in every deck. Create a new deck to add it again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                        modifier = Modifier.padding(
                            horizontal = MindVaultSpacing.screenHorizontal,
                            vertical = MindVaultSpacing.space2,
                        ),
                    )
                }
                TextButton(
                    onClick = { showNewDeckDialog = true },
                    modifier = Modifier.padding(horizontal = MindVaultSpacing.space2),
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(MindVaultSpacing.space2))
                    Text("New deck")
                }
            }
        }

        state.error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    horizontal = MindVaultSpacing.screenHorizontal,
                    vertical = MindVaultSpacing.space2,
                ),
            )
        }

        PrimaryButton(
            text = if (state.isAdding) "Adding…" else "Add to deck",
            onClick = viewModel::onAdd,
            enabled = state.canAdd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MindVaultSpacing.screenHorizontal, vertical = MindVaultSpacing.space3),
        )
        Spacer(Modifier.height(MindVaultSpacing.space4))
    }

    if (showNewDeckDialog) {
        DeckNameDialog(
            title = "New deck",
            confirmLabel = "Create",
            initialName = "",
            error = state.form.error,
            isSaving = state.form.isSaving,
            onConfirm = viewModel::onCreateDeck,
            onDismiss = {
                showNewDeckDialog = false
                viewModel.onNewDeckFormDismissed()
            },
        )
    }
}

@Composable
private fun DeckOptionRow(
    option: DeckOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val enabled = !option.containsItem
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MindVaultSpacing.listItemMinHeight)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = MindVaultSpacing.screenHorizontal, vertical = MindVaultSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MindVaultColors.Forest,
                unselectedColor = MindVaultColors.TextSecondary,
                disabledSelectedColor = MindVaultColors.Disabled,
                disabledUnselectedColor = MindVaultColors.Disabled,
            ),
        )
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
            Text(
                text = option.deck.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MindVaultColors.TextPrimary else MindVaultColors.TextSecondary,
            )
            if (option.containsItem) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MindVaultColors.Forest,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(MindVaultSpacing.space1))
                    Text(
                        text = "Already added",
                        style = MaterialTheme.typography.labelMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
            }
        }
    }
}