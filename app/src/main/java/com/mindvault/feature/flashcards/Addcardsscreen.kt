package com.mindvault.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultSearchField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton

@Composable
fun AddCardsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCardsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBackClick)

    LaunchedEffect(state.isDone) {
        if (state.isDone) currentOnBack()
    }

    Column(modifier.fillMaxSize().imePadding()) {
        MindVaultTopBar(title = "Add cards", onBackClick = onBackClick)

        when (state.status) {
            AddCardsStatus.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            AddCardsStatus.NotFound -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Deck not found",
                    message = "This deck may have been deleted.",
                )
            }

            AddCardsStatus.Error -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load your Vault",
                    message = "Go back and try again.",
                )
            }

            AddCardsStatus.Ready -> {
                Text(
                    text = "Adding to “${state.deckName}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = MindVaultSpacing.screenHorizontal),
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))
                MindVaultSearchField(
                    value = viewModel.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search your Vault",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MindVaultSpacing.screenHorizontal),
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))

                when {
                    state.totalSaved == 0 -> MessageBody {
                        EmptyState(
                            icon = Icons.Outlined.Style,
                            title = "Your Vault is empty",
                            message = "Save a word or concept first, then come back to turn it into a card.",
                        )
                    }

                    state.entries.isEmpty() -> MessageBody {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matches",
                            message = "Nothing in your Vault matches that search.",
                        )
                    }

                    else -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.entries, key = { it.entry.item.id }) { picker ->
                            PickerRow(
                                entry = picker,
                                checked = picker.inDeck || picker.entry.item.id in state.selectedIds,
                                onToggle = { viewModel.onToggle(picker.entry.item.id) },
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MindVaultSpacing.screenHorizontal,
                            vertical = MindVaultSpacing.space3,
                        ),
                ) {
                    state.error?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = MindVaultSpacing.space2),
                        )
                    }
                    val count = state.selectedIds.size
                    PrimaryButton(
                        text = when {
                            state.isAdding -> "Adding…"
                            count == 0 -> "Add cards"
                            else -> "Add ${cardsLabel(count)}"
                        },
                        onClick = viewModel::onAdd,
                        enabled = count > 0 && !state.isAdding,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    entry: PickerEntry,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val item = entry.entry.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MindVaultSpacing.listItemMinHeight)
            .toggleable(
                value = checked,
                enabled = !entry.inDeck,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = MindVaultSpacing.screenHorizontal, vertical = MindVaultSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = !entry.inDeck,
            colors = CheckboxDefaults.colors(
                checkedColor = MindVaultColors.Forest,
                checkmarkColor = MindVaultColors.TextOnPrimary,
                uncheckedColor = MindVaultColors.TextSecondary,
                disabledCheckedColor = MindVaultColors.Disabled,
                disabledUncheckedColor = MindVaultColors.Disabled,
            ),
        )
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.inDeck) MindVaultColors.TextSecondary else MindVaultColors.TextPrimary,
            )
            Text(
                text = if (entry.inDeck) "Already in this deck" else item.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.MessageBody(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
        content = content,
    )
}