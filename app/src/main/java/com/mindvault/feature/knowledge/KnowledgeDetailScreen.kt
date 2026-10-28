package com.mindvault.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.VaultEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun KnowledgeDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KnowledgeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showAddToDeck by rememberSaveable { mutableStateOf(false) }
    val currentOnBack by rememberUpdatedState(onBackClick)

    LaunchedEffect(deleteState.isDeleted) {
        if (deleteState.isDeleted) currentOnBack()
    }

    val entry = (state as? KnowledgeDetailUiState.Content)?.entry

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(
            title = "Details",
            onBackClick = onBackClick,
            actions = {
                if (entry != null) {
                    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.TextPrimary)
                    IconButton(onClick = { showAddToDeck = true }, colors = iconColors) {
                        Icon(Icons.Outlined.Style, contentDescription = "Add to flashcards")
                    }
                    IconButton(onClick = { onEditClick(entry.item.id) }, colors = iconColors) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }, colors = iconColors) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            },
        )

        when (val current = state) {
            KnowledgeDetailUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            KnowledgeDetailUiState.NotFound -> CenteredMessage(
                icon = Icons.Outlined.SearchOff,
                title = "Item not found",
                message = "This item may have been removed.",
                modifier = Modifier.weight(1f),
            )

            KnowledgeDetailUiState.Error -> CenteredMessage(
                icon = Icons.Outlined.ErrorOutline,
                title = "Couldn't load this item",
                message = "Go back and try opening it again.",
                modifier = Modifier.weight(1f),
            )

            is KnowledgeDetailUiState.Content -> DetailContent(
                entry = current.entry,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showAddToDeck) {
        AddToDeckSheet(onDismiss = { showAddToDeck = false })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!deleteState.isDeleting) {
                    showDeleteDialog = false
                    viewModel.onDeleteDialogDismissed()
                }
            },
            containerColor = MindVaultColors.Surface,
            title = { Text("Delete this item?", color = MindVaultColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "“${entry?.item?.title ?: "This item"}” will be permanently removed from your " +
                                "vault and from any flashcard decks. This can't be undone.",
                        color = MindVaultColors.TextSecondary,
                    )
                    deleteState.error?.let { message ->
                        Spacer(Modifier.height(MindVaultSpacing.space2))
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirmed,
                    enabled = !deleteState.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(if (deleteState.isDeleting) "Deleting…" else "Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDeleteDialogDismissed()
                    },
                    enabled = !deleteState.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CenteredMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
    ) {
        EmptyState(icon = icon, title = title, message = message)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    entry: VaultEntry,
    modifier: Modifier = Modifier,
) {
    val item = entry.item
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = MindVaultSpacing.screenHorizontal,
                end = MindVaultSpacing.screenHorizontal,
                bottom = MindVaultSpacing.space6,
            ),
    ) {
        // Selectable so definitions and notes can be copied.
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space5)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MindVaultColors.TextPrimary,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
                    verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
                ) {
                    MetaTag(item.type.label())
                    MetaTag(entry.categoryName ?: "Uncategorized")
                }

                DetailSection(label = "Definition", body = item.definition)

                item.personalNotes.takeIf { it.isNotBlank() }
                    ?.let { DetailSection(label = "Personal notes", body = it) }

                item.example?.takeIf { it.isNotBlank() }
                    ?.let { DetailSection(label = "Example", body = it) }

                Column {
                    Text(
                        text = "Added ${dateFormat.format(Date(item.createdAt))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                    if (item.updatedAt != item.createdAt) {
                        Text(
                            text = "Updated ${dateFormat.format(Date(item.updatedAt))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MindVaultColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(label: String, body: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MindVaultColors.TextSecondary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MindVaultColors.TextPrimary,
        )
    }
}

@Composable
private fun MetaTag(text: String) {
    Surface(shape = ShapePill, color = MindVaultColors.SurfaceVariant) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MindVaultColors.TextPrimary,
            modifier = Modifier.padding(
                horizontal = MindVaultSpacing.space3,
                vertical = MindVaultSpacing.space1,
            ),
        )
    }
}