package com.mindvault.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeMedium
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.DeckSummary

/** Deck list: counts and progress per deck, with a "New deck" action. Opens a deck on tap. */
@Composable
fun FlashcardsScreen(
    onDeckClick: (deckId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlashcardsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.deckForm.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(form.isDone) {
        if (form.isDone) {
            showCreateDialog = false
            viewModel.onDeckFormDismissed()
        }
    }

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(
            title = "Flashcards",
            actions = {
                IconButton(
                    onClick = { showCreateDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.TextPrimary),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "New deck")
                }
            },
        )

        when (val current = state) {
            DecksUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            DecksUiState.Error -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MindVaultSpacing.screenHorizontal),
            ) {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load your decks",
                    message = "Close and reopen the app, then try again.",
                )
            }

            is DecksUiState.Content -> DeckList(
                decks = current.decks,
                onDeckClick = onDeckClick,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showCreateDialog) {
        DeckNameDialog(
            title = "New deck",
            confirmLabel = "Create",
            initialName = "",
            error = form.error,
            isSaving = form.isSaving,
            onConfirm = viewModel::onCreateDeck,
            onDismiss = {
                showCreateDialog = false
                viewModel.onDeckFormDismissed()
            },
        )
    }
}

@Composable
private fun DeckList(
    decks: List<DeckSummary>,
    onDeckClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = MindVaultSpacing.screenHorizontal,
            end = MindVaultSpacing.screenHorizontal,
            bottom = MindVaultSpacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
    ) {
        if (decks.sumOf { it.totalCards } == 0) {
            item(key = "hint") {
                Text(
                    text = "Open a saved item and choose Add to flashcards to make your first card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
        items(decks, key = { it.deck.id }) { summary ->
            DeckRow(summary = summary, onClick = { onDeckClick(summary.deck.id) })
        }
    }
}

@Composable
private fun DeckRow(summary: DeckSummary, onClick: () -> Unit) {
    MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ShapeMedium)
                    .background(MindVaultColors.SageLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Style,
                    contentDescription = null,
                    tint = MindVaultColors.Forest,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(MindVaultSpacing.space3))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MindVaultColors.TextPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (summary.deck.isDefault) {
                        Spacer(Modifier.width(MindVaultSpacing.space2))
                        DeckTag("Default")
                    }
                }
                DeckProgress(summary)
            }
        }
    }
}