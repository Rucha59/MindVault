package com.mindvault.feature.vault

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultFilterChip
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.Category
import com.mindvault.domain.model.CategoryFilter
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.model.VaultSort
import com.mindvault.feature.knowledge.label
import com.mindvault.core.ui.components.MindVaultSearchField

/** Library-first Vault: search, type and category filters, sort, and the list of saved items. */
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

        when (state.status) {
            VaultStatus.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            VaultStatus.Error -> CenteredMessage(
                modifier = Modifier.weight(1f),
            ) {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load your vault",
                    message = "Something went wrong reading your saved items. Close and reopen the app to try again.",
                )
            }

            VaultStatus.Ready -> if (state.totalCount == 0) {
                CenteredMessage(modifier = Modifier.weight(1f)) {
                    EmptyState(
                        icon = Icons.Outlined.Bookmarks,
                        title = "Your vault is empty",
                        message = "Words and concepts you save will appear here.",
                        actionLabel = "Add a concept",
                        onAction = onAddConceptClick,
                    )
                }
            } else {
                VaultBrowser(
                    state = state,
                    searchText = viewModel.searchText,
                    onSearchChange = viewModel::onSearchChange,
                    onTypeChange = viewModel::onTypeFilterChange,
                    onCategoryChange = viewModel::onCategoryFilterChange,
                    onSortChange = viewModel::onSortChange,
                    onClearFilters = viewModel::onClearFilters,
                    onItemClick = onItemClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
    ) {
        content()
    }
}

@Composable
private fun VaultBrowser(
    state: VaultUiState,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onTypeChange: (KnowledgeType?) -> Unit,
    onCategoryChange: (CategoryFilter) -> Unit,
    onSortChange: (VaultSort) -> Unit,
    onClearFilters: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query = state.query
    val focusManager = LocalFocusManager.current
    Column(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = MindVaultSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
        ) {
            MindVaultSearchField(
                value = searchText,
                onValueChange = onSearchChange,
                placeholder = "Search your vault",
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
            ) {
                MindVaultFilterChip(
                    label = "All types",
                    selected = query.type == null,
                    onClick = { onTypeChange(null) },
                )
                KnowledgeType.entries.forEach { type ->
                    MindVaultFilterChip(
                        label = type.label(),
                        selected = query.type == type,
                        onClick = { onTypeChange(type) },
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                val categoryOptions = listOf<CategoryFilter>(CategoryFilter.All, CategoryFilter.Uncategorized) +
                        state.categories.map { CategoryFilter.Specific(it.id) }
                DropdownSelector(
                    label = "Category",
                    selectedText = query.category.label(state.categories),
                    options = categoryOptions,
                    optionLabel = { it.label(state.categories) },
                    isSelected = { it == query.category },
                    onSelect = onCategoryChange,
                    modifier = Modifier.weight(1f),
                )
                DropdownSelector(
                    label = "Sort",
                    selectedText = query.sort.label(),
                    options = VaultSort.entries,
                    optionLabel = { it.label() },
                    isSelected = { it == query.sort },
                    onSelect = onSortChange,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = countText(shown = state.results.size, total = state.totalCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MindVaultColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (query.hasActiveFilters) {
                    TextButton(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("Clear filters") }
                }
            }
        }

        if (state.results.isEmpty()) {
            CenteredMessage(modifier = Modifier.weight(1f)) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No matches",
                    message = "Nothing in your vault matches this search and these filters.",
                    actionLabel = "Clear filters",
                    onAction = onClearFilters,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MindVaultSpacing.screenHorizontal,
                    end = MindVaultSpacing.screenHorizontal,
                    top = MindVaultSpacing.space1,
                    bottom = MindVaultSpacing.space4,
                ),
                verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
            ) {
                items(state.results, key = { it.item.id }) { entry ->
                    VaultItemCard(entry, onClick = { onItemClick(entry.item.id) })
                }
            }
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
        ) {
            Text(
                text = "$label: $selectedText",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    trailingIcon = if (isSelected(option)) {
                        ({ Icon(Icons.Filled.Check, contentDescription = "Selected") })
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
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

private fun countText(shown: Int, total: Int): String = when {
    shown == total -> if (total == 1) "1 item" else "$total items"
    else -> "$shown of $total items"
}

private fun CategoryFilter.label(categories: List<Category>): String = when (this) {
    CategoryFilter.All -> "All"
    CategoryFilter.Uncategorized -> "Uncategorized"
    is CategoryFilter.Specific -> categories.firstOrNull { it.id == categoryId }?.name ?: "Unknown"
}

private fun VaultSort.label(): String = when (this) {
    VaultSort.RECENTLY_UPDATED -> "Recently updated"
    VaultSort.RECENTLY_ADDED -> "Recently added"
    VaultSort.A_TO_Z -> "A–Z"
    VaultSort.Z_TO_A -> "Z–A"
}