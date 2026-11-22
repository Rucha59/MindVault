package com.mindvault.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultTextField
import com.mindvault.domain.model.Deck

/** The "⋮" menu next to the Vault filters when one category is selected. */
@Composable
internal fun CategoryActionsMenu(
    hasDeck: Boolean,
    onRenameClick: () -> Unit,
    onCreateDeckClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(
            onClick = { expanded = true },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.Forest),
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Category actions")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename category") },
                onClick = {
                    expanded = false
                    onRenameClick()
                },
            )
            if (!hasDeck) {
                DropdownMenuItem(
                    text = { Text("Create flashcard deck") },
                    onClick = {
                        expanded = false
                        onCreateDeckClick()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Delete category", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
internal fun RenameCategoryDialog(
    initialName: String,
    error: String?,
    isSaving: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MindVaultColors.Surface,
        title = { Text("Rename category", color = MindVaultColors.TextPrimary) },
        text = {
            MindVaultTextField(
                value = name,
                onValueChange = { name = it },
                label = "Category name",
                singleLine = true,
                errorText = error,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text(if (isSaving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Cancel") }
        },
    )
}

/** Deleting a category keeps its items. If it has a deck, the user chooses to keep it (default) or delete it too. */
@Composable
internal fun DeleteCategoryDialog(
    categoryName: String,
    deck: Deck?,
    error: String?,
    isDeleting: Boolean,
    onConfirm: (deleteDeck: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteDeck by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = MindVaultColors.Surface,
        title = { Text("Delete “$categoryName”?", color = MindVaultColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
                Text(
                    text = "Its items stay in your Vault as Uncategorized.",
                    color = MindVaultColors.TextSecondary,
                )
                if (deck != null) {
                    Column(Modifier.selectableGroup()) {
                        DeckChoiceRow(
                            selected = !deleteDeck,
                            enabled = !isDeleting,
                            label = "Keep the “${deck.name}” deck as an ordinary deck",
                            onClick = { deleteDeck = false },
                        )
                        DeckChoiceRow(
                            selected = deleteDeck,
                            enabled = !isDeleting,
                            label = "Delete the deck too (its cards and review progress are lost)",
                            onClick = { deleteDeck = true },
                        )
                    }
                }
                error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteDeck && deck != null) },
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(if (isDeleting) "Deleting…" else "Delete") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeckChoiceRow(selected: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = MindVaultColors.Forest),
        )
        Spacer(Modifier.width(MindVaultSpacing.space2))
        Text(text = label, color = MindVaultColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}