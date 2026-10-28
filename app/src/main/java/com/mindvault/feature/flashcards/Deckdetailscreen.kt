package com.mindvault.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.core.ui.components.SecondaryButton
import com.mindvault.domain.model.DeckDetail
import com.mindvault.domain.model.FlashcardDetail

@Composable
fun DeckDetailScreen(
    onBackClick: () -> Unit,
    onStudyClick: (deckId: String, shuffle: Boolean) -> Unit,
    onAddCardsClick: (deckId: String) -> Unit,
    onCardClick: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var cardToRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnBack by rememberUpdatedState(onBackClick)

    LaunchedEffect(actions.isDeleted) {
        if (actions.isDeleted) currentOnBack()
    }
    LaunchedEffect(actions.form.isDone) {
        if (actions.form.isDone) {
            showRenameDialog = false
            viewModel.onRenameDismissed()
        }
    }

    val detail = (state as? DeckDetailUiState.Content)?.detail

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(
            title = detail?.deck?.name ?: "Deck",
            onBackClick = onBackClick,
            actions = {
                if (detail != null) {
                    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.TextPrimary)
                    IconButton(onClick = { showRenameDialog = true }, colors = iconColors) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Rename deck")
                    }
                    // The default deck can never be deleted, so the action isn't offered.
                    if (!detail.deck.isDefault) {
                        IconButton(onClick = { showDeleteDialog = true }, colors = iconColors) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete deck")
                        }
                    }
                }
            },
        )

        when (val current = state) {
            DeckDetailUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            DeckDetailUiState.NotFound -> CenteredMessage(
                icon = Icons.Outlined.SearchOff,
                title = "Deck not found",
                message = "This deck may have been deleted.",
                modifier = Modifier.weight(1f),
            )

            DeckDetailUiState.Error -> CenteredMessage(
                icon = Icons.Outlined.ErrorOutline,
                title = "Couldn't load this deck",
                message = "Go back and try opening it again.",
                modifier = Modifier.weight(1f),
            )

            is DeckDetailUiState.Content -> DeckContent(
                detail = current.detail,
                message = actions.message,
                onStudyClick = { shuffle -> onStudyClick(current.detail.deck.id, shuffle) },
                onAddCardsClick = { onAddCardsClick(current.detail.deck.id) },
                onCardClick = onCardClick,
                onRemoveCard = { cardToRemoveId = it },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showRenameDialog && detail != null) {
        DeckNameDialog(
            title = "Rename deck",
            confirmLabel = "Save",
            initialName = detail.deck.name,
            error = actions.form.error,
            isSaving = actions.form.isSaving,
            onConfirm = viewModel::onRename,
            onDismiss = {
                showRenameDialog = false
                viewModel.onRenameDismissed()
            },
        )
    }

    if (showDeleteDialog && detail != null) {
        AlertDialog(
            onDismissRequest = {
                if (!actions.isDeleting) {
                    showDeleteDialog = false
                    viewModel.onDeleteDismissed()
                }
            },
            containerColor = MindVaultColors.Surface,
            title = { Text("Delete this deck?", color = MindVaultColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "“${detail.deck.name}” and its ${cardsLabel(detail.summary.totalCards)} will be " +
                                "removed, along with their review progress. Your saved items stay in your Vault.",
                        color = MindVaultColors.TextSecondary,
                    )
                    actions.deleteError?.let { message ->
                        Spacer(Modifier.height(MindVaultSpacing.space2))
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirmed,
                    enabled = !actions.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(if (actions.isDeleting) "Deleting…" else "Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDeleteDismissed()
                    },
                    enabled = !actions.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Cancel") }
            },
        )
    }

    val cardToRemove = detail?.cards?.firstOrNull { it.flashcard.id == cardToRemoveId }
    if (cardToRemove != null) {
        AlertDialog(
            onDismissRequest = { cardToRemoveId = null },
            containerColor = MindVaultColors.Surface,
            title = { Text("Remove this card?", color = MindVaultColors.TextPrimary) },
            text = {
                Text(
                    text = "“${cardToRemove.knowledgeItem.title}” will be removed from this deck, and its review " +
                            "progress here is lost. The saved item stays in your Vault.",
                    color = MindVaultColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onRemoveCard(cardToRemove.flashcard.id)
                        cardToRemoveId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { cardToRemoveId = null },
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

@Composable
private fun DeckContent(
    detail: DeckDetail,
    message: String?,
    onStudyClick: (shuffle: Boolean) -> Unit,
    onAddCardsClick: () -> Unit,
    onCardClick: (itemId: String) -> Unit,
    onRemoveCard: (flashcardId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCards = detail.cards.isNotEmpty()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = MindVaultSpacing.screenHorizontal,
            end = MindVaultSpacing.screenHorizontal,
            bottom = MindVaultSpacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space4)) {
                DeckProgress(detail.summary)
                Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
                    PrimaryButton(
                        text = "Study",
                        onClick = { onStudyClick(false) },
                        enabled = hasCards,
                        icon = Icons.Outlined.Style,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Shuffle",
                        onClick = { onStudyClick(true) },
                        enabled = hasCards,
                        icon = Icons.Outlined.Shuffle,
                        modifier = Modifier.weight(1f),
                    )
                }
                SecondaryButton(
                    text = "Add cards",
                    onClick = onAddCardsClick,
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        message?.let {
            item(key = "message") {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        if (!hasCards) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.Style,
                    title = "No cards in this deck",
                    message = "Add cards from your Vault to start studying.",
                )
            }
        } else {
            items(detail.cards, key = { it.flashcard.id }) { card ->
                DeckCardRow(
                    card = card,
                    onClick = { onCardClick(card.knowledgeItem.id) },
                    onRemove = { onRemoveCard(card.flashcard.id) },
                )
            }
        }
    }
}

@Composable
private fun DeckCardRow(
    card: FlashcardDetail,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val reviewed = card.reviewState.isReviewed
    MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
                Text(
                    text = card.knowledgeItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Text(
                    text = card.knowledgeItem.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reviewed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (reviewed) MindVaultColors.Forest else MindVaultColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(MindVaultSpacing.space1))
                    Text(
                        text = if (reviewed) "Reviewed" else "Not reviewed yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
            }
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.TextSecondary),
            ) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove from deck")
            }
        }
    }
}