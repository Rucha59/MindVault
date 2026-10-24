package com.mindvault.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultSearchField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.toSaveContent
import com.mindvault.domain.model.toTitleKey

/** Lookup-first Search: query, then loading / results / no result / offline / error. */
@Composable
fun SearchScreen(
    onAddConceptClick: () -> Unit,
    onViewSavedClick: (itemId: String) -> Unit,
    onEditBeforeSaveClick: (title: String, definition: String, example: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Search")
        MindVaultSearchField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search for a word",
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.onSearch()
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        )
        Spacer(Modifier.height(MindVaultSpacing.space4))

        when (state.status) {
            SearchStatus.Idle -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Look up a word",
                    message = "Search for a word, then save it to your Vault. Definitions come from an online dictionary, so you\u2019ll need a connection. For concepts it doesn\u2019t cover, add your own.",
                    actionLabel = "Add a concept",
                    onAction = onAddConceptClick,
                )
            }

            SearchStatus.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MindVaultColors.Forest)
                    Spacer(Modifier.height(MindVaultSpacing.space3))
                    Text(
                        text = "Searching\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
            }

            SearchStatus.NoResults -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No definition found",
                    message = "Nothing matched \u201C${state.submittedQuery}\u201D. Check the spelling, or add it yourself.",
                    actionLabel = "Add a concept",
                    onAction = onAddConceptClick,
                )
            }

            SearchStatus.Offline -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.WifiOff,
                    title = "Can\u2019t reach the dictionary",
                    message = "Check your connection and try again. Your Vault still works offline.",
                    actionLabel = "Try again",
                    onAction = viewModel::onSearch,
                )
            }

            SearchStatus.Error -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Search isn\u2019t working right now",
                    message = "Something went wrong on the dictionary\u2019s side. Try again, or add the concept yourself.",
                    actionLabel = "Try again",
                    onAction = viewModel::onSearch,
                )
            }

            SearchStatus.Results -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = MindVaultSpacing.screenHorizontal,
                    end = MindVaultSpacing.screenHorizontal,
                    bottom = MindVaultSpacing.space4,
                ),
                verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
            ) {
                state.saveError?.let { message ->
                    item(key = "save-error") {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(state.results, key = { it.result.title.toTitleKey() }) { item ->
                    SearchResultCard(
                        item = item,
                        isSaving = item.result.title.toTitleKey() in state.savingKeys,
                        onSave = { viewModel.onSave(item.result) },
                        onEditFirst = {
                            val content = item.result.toSaveContent()
                            onEditBeforeSaveClick(content.title, content.definition, content.example)
                        },
                        onViewSaved = onViewSavedClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.MessageBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
        content = content,
    )
}