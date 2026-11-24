package com.mindvault.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeMedium
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultFilterChip
import com.mindvault.core.ui.components.MindVaultSearchField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.model.savedItemKey

/**
 * Lookup-first Search: query and filter (All / Words / Concepts), then loading / results / no result /
 * offline / error. When only one source fails, the other's results still show with a notice and a Retry.
 */
@Composable
fun SearchScreen(
    onAddConceptClick: () -> Unit,
    onViewSavedClick: (itemId: String) -> Unit,
    /** Opens the Add form with the result; [type] is a KnowledgeType name (WORD or CONCEPT). */
    onEditBeforeSaveClick: (title: String, definition: String, example: String?, type: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val currentOnEdit by rememberUpdatedState(onEditBeforeSaveClick)
    var fieldFocused by remember { mutableStateOf(false) }
    // Suggestions replace the results area while the field is focused; recent searches show for an empty field.
    val showSuggestions = fieldFocused && viewModel.query.isNotBlank() && state.suggestions.isNotEmpty()
    val showRecent = fieldFocused && viewModel.query.isBlank() && state.recentSearches.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenEditForm -> currentOnEdit(
                    event.content.title,
                    event.content.definition,
                    event.content.example,
                    event.content.type.name,
                )
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Search")
        MindVaultSearchField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search for a word or concept",
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.onSearch()
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MindVaultSpacing.screenHorizontal)
                .onFocusChanged { fieldFocused = it.hasFocus },
        )
        Spacer(Modifier.height(MindVaultSpacing.space3))
        ScopeFilters(
            selected = state.scope,
            onSelected = viewModel::onScopeChange,
            modifier = Modifier.padding(horizontal = MindVaultSpacing.screenHorizontal),
        )
        Spacer(Modifier.height(MindVaultSpacing.space4))

        when {
            showSuggestions -> TermList(
                title = null,
                terms = state.suggestions,
                showHistoryIcon = false,
                onTermClick = { term ->
                    focusManager.clearFocus()
                    viewModel.onSuggestionSelected(term)
                },
            )

            showRecent -> TermList(
                title = "Recent searches",
                terms = state.recentSearches,
                showHistoryIcon = true,
                onClear = viewModel::onClearRecentSearches,
                onTermClick = { term ->
                    focusManager.clearFocus()
                    viewModel.onSuggestionSelected(term)
                },
            )

            else -> SearchBody(
                state = state,
                viewModel = viewModel,
                onAddConceptClick = onAddConceptClick,
                onViewSavedClick = onViewSavedClick,
            )
        }
    }
}

/** Everything under the filters when no suggestion or recent-search list is showing. */
@Composable
private fun ColumnScope.SearchBody(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onAddConceptClick: () -> Unit,
    onViewSavedClick: (itemId: String) -> Unit,
) {
    run {
        when (state.status) {
            SearchStatus.Idle -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Look up a word or concept",
                    message = "Search for a word or a concept, then save it to your Vault. Words come from Wiktionary and concepts from Wikipedia, so you\u2019ll need a connection. Anything they don\u2019t cover, add yourself.",
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
                state.notices.forEach { notice ->
                    SourceNoticeRow(notice, onRetry = { viewModel.onRetrySource(notice.source) })
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                }
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Nothing found",
                    message = "Nothing matched \u201C${state.submittedQuery}\u201D. Check the spelling, try another filter, or add it yourself.",
                    actionLabel = "Add a concept",
                    onAction = onAddConceptClick,
                )
            }

            SearchStatus.Offline -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.WifiOff,
                    title = "Can\u2019t connect",
                    message = "Check your connection and try again. Your Vault still works offline.",
                    actionLabel = "Try again",
                    onAction = viewModel::onSearch,
                )
            }

            SearchStatus.Error -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Search isn\u2019t working right now",
                    message = "The source may be busy or something went wrong on its side. Try again in a moment, or add the concept yourself.",
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
                items(state.notices, key = { "notice-${it.source.name}" }) { notice ->
                    SourceNoticeRow(notice, onRetry = { viewModel.onRetrySource(notice.source) })
                }
                items(state.results, key = { it.result.savedItemKey() }) { item ->
                    SearchResultCard(
                        item = item,
                        isSaving = item.result.savedItemKey() in state.savingKeys,
                        onToggleExpanded = { viewModel.onToggleExpanded(item.result) },
                        onRetrySummary = { viewModel.onLoadSummary(item.result) },
                        onSave = { viewModel.onSave(item.result) },
                        onEditFirst = { viewModel.onEditFirst(item.result) },
                        onViewSaved = onViewSavedClick,
                    )
                }
            }
        }
    }
}

/** Autocomplete suggestions or recent searches: one tappable row per term. */
@Composable
private fun ColumnScope.TermList(
    title: String?,
    terms: List<String>,
    showHistoryIcon: Boolean,
    onTermClick: (String) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = MindVaultSpacing.screenHorizontal, end = MindVaultSpacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MindVaultColors.TextSecondary,
                )
                if (onClear != null) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("Clear") }
                }
            }
        }
        terms.forEach { term ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { onTermClick(term) }
                    .padding(horizontal = MindVaultSpacing.screenHorizontal, vertical = MindVaultSpacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (showHistoryIcon) Icons.Outlined.History else Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MindVaultColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(MindVaultSpacing.space3))
                Text(
                    text = term,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindVaultColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun ScopeFilters(
    selected: SearchScope,
    onSelected: (SearchScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
        SearchScope.entries.forEach { scope ->
            MindVaultFilterChip(
                label = when (scope) {
                    SearchScope.ALL -> "All"
                    SearchScope.WORDS -> "Words"
                    SearchScope.CONCEPTS -> "Concepts"
                },
                selected = scope == selected,
                onClick = { onSelected(scope) },
            )
        }
    }
}

/** Shown when one source failed while the other answered: says what is missing and offers Retry for that source. */
@Composable
private fun SourceNoticeRow(notice: SourceNotice, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MindVaultColors.SurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                start = MindVaultSpacing.space3,
                top = MindVaultSpacing.space1,
                bottom = MindVaultSpacing.space1,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = notice.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
                enabled = !notice.isRetrying,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text(if (notice.isRetrying) "Retrying\u2026" else "Retry") }
        }
    }
}

private fun SourceNotice.message(): String {
    val name = if (source == SearchSource.WORDS) "Wiktionary" else "Wikipedia"
    val missing = if (source == SearchSource.WORDS) "word definitions" else "concepts"
    return when (reason) {
        SourceFailureReason.OFFLINE -> "Couldn\u2019t reach $name, so $missing are missing. Check your connection."
        SourceFailureReason.BUSY -> "$name is busy right now, so $missing are missing."
        SourceFailureReason.ERROR -> "$name didn\u2019t answer properly, so $missing are missing."
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