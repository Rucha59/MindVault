package com.mindvault.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.VaultEntry
import com.mindvault.feature.knowledge.label

/**
 * Library-first Vault. Phase 3 shows saved items read-only. Search, filter chips, sorting,
 * detail, edit and delete are added in the next phase.
 */
@Composable
fun VaultScreen(
    onAddConceptClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(
            title = "My Vault",
            actions = {
                IconButton(
                    onClick = onAddConceptClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.Forest),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add a concept")
                }
            },
        )

        when (val current = state) {
            VaultUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            VaultUiState.Empty -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MindVaultSpacing.screenHorizontal),
            ) {
                EmptyState(
                    icon = Icons.Outlined.Bookmarks,
                    title = "Your vault is empty",
                    message = "Words and concepts you save will appear here.",
                    actionLabel = "Add a concept",
                    onAction = onAddConceptClick,
                )
            }

            VaultUiState.Error -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MindVaultSpacing.screenHorizontal),
            ) {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load your vault",
                    message = "Something went wrong reading your saved items. Close and reopen the app to try again.",
                )
            }

            is VaultUiState.Content -> VaultList(
                entries = current.entries,
                onItemClick = onItemClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VaultList(
    entries: List<VaultEntry>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
)  {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = MindVaultSpacing.screenHorizontal,
            end = MindVaultSpacing.screenHorizontal,
            bottom = MindVaultSpacing.space4,
        ),
        verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
    ) {
        item(key = "count") {
            Text(
                text = if (entries.size == 1) "1 item" else "${entries.size} items",
                style = MaterialTheme.typography.labelLarge,
                color = MindVaultColors.TextSecondary,
            )
        }
        items(entries, key = { it.item.id }) { entry ->
            VaultItemCard(entry, onClick = { onItemClick(entry.item.id) })
        }
    }
}

@Composable
private fun VaultItemCard(entry: VaultEntry, onClick: () -> Unit) {
    val item = entry.item
    val meta = listOfNotNull(item.type.label(), entry.categoryName).joinToString(" · ")

    MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MindVaultColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = item.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium,
            color = MindVaultColors.TextSecondary,
        )
    }
}