package com.mindvault.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.AccentColors
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge
import com.mindvault.core.theme.ShapeMedium
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.model.HomeSummary
import com.mindvault.domain.model.VaultEntry
import com.mindvault.feature.flashcards.DeckProgress

/**
 * Dashboard-first Home. Everything below the quick actions comes from saved data; a section with
 * nothing real to show is hidden, and a new user sees an honest empty state.
 */
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onAddConceptClick: () -> Unit,
    onVaultClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onItemClick: (itemId: String) -> Unit,
    onDeckClick: (deckId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(
            title = "MindVault",
            actions = {
                IconButton(
                    onClick = onProfileClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MindVaultColors.TextPrimary),
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = "Profile")
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            SearchShortcut(onClick = onSearchClick)
            Spacer(Modifier.height(MindVaultSpacing.space5))
            WelcomeCard(onAddConceptClick = onAddConceptClick)
            Spacer(Modifier.height(MindVaultSpacing.space6))

            SectionHeader("Quick actions")
            QuickActions(
                onAddConceptClick = onAddConceptClick,
                onSearchClick = onSearchClick,
                onFlashcardsClick = onFlashcardsClick,
                onVaultClick = onVaultClick,
            )
            Spacer(Modifier.height(MindVaultSpacing.space6))

            when (val current = state) {
                HomeUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MindVaultColors.Forest)
                }

                HomeUiState.Error -> EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load your library",
                    message = "Close and reopen the app, then try again.",
                )

                is HomeUiState.Content -> HomeContent(
                    summary = current.summary,
                    onItemClick = onItemClick,
                    onDeckClick = onDeckClick,
                    onFlashcardsClick = onFlashcardsClick,
                    onVaultClick = onVaultClick,
                )
            }
            Spacer(Modifier.height(MindVaultSpacing.space4))
        }
    }
}

@Composable
private fun HomeContent(
    summary: HomeSummary,
    onItemClick: (String) -> Unit,
    onDeckClick: (String) -> Unit,
    onFlashcardsClick: () -> Unit,
    onVaultClick: () -> Unit,
) {
    if (summary.totalItems == 0) {
        SectionHeader("Recently saved")
        EmptyState(
            icon = Icons.Outlined.Bookmarks,
            title = "Nothing saved yet",
            message = "Words and concepts you save will appear here.",
        )
        return
    }

    if (summary.hasStudyStatus) {
        StudySection(summary, onDeckClick = onDeckClick, onFlashcardsClick = onFlashcardsClick)
        Spacer(Modifier.height(MindVaultSpacing.space6))
    }

    SectionHeader("Categories")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
    ) {
        summary.categoryCounts.forEach { Pill("${it.name} · ${it.count}") }
    }
    Spacer(Modifier.height(MindVaultSpacing.space6))

    SectionHeaderWithAction(title = "Recently saved", actionLabel = "See all", onAction = onVaultClick)
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
        summary.recentItems.forEach { entry ->
            RecentItemRow(entry = entry, onClick = { onItemClick(entry.item.id) })
        }
    }
}

@Composable
private fun StudySection(
    summary: HomeSummary,
    onDeckClick: (String) -> Unit,
    onFlashcardsClick: () -> Unit,
) {
    SectionHeader(if (summary.inProgressDecks.isNotEmpty()) "Continue studying" else "Flashcards")
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
        summary.inProgressDecks.forEach { deck ->
            MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = { onDeckClick(deck.deck.id) }) {
                Text(
                    text = deck.deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))
                DeckProgress(deck)
            }
        }
        if (summary.inProgressDecks.isEmpty() && summary.hasUnstartedDeck) {
            MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = onFlashcardsClick) {
                Text(
                    text = "Start studying",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    text = "Pick a deck and flip your first card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
        if (summary.decksCompletedToday > 0) {
            val n = summary.decksCompletedToday
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MindVaultColors.Forest,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MindVaultSpacing.space2))
                Text(
                    text = if (n == 1) "1 deck completed today" else "$n decks completed today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderWithAction(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MindVaultColors.TextPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        TextButton(
            onClick = onAction,
            colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecentItemRow(entry: VaultEntry, onClick: () -> Unit) {
    MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = entry.item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MindVaultColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = entry.item.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        entry.categoryName?.let { name ->
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Pill(name)
        }
    }
}

@Composable
private fun Pill(text: String) {
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

@Composable
private fun SearchShortcut(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeLarge,
        color = MindVaultColors.Surface,
        border = BorderStroke(1.dp, MindVaultColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = "Open search", onClick = onClick)
                .padding(horizontal = MindVaultSpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MindVaultColors.TextSecondary)
            Spacer(Modifier.width(MindVaultSpacing.space3))
            Text(
                text = "Search words and concepts",
                style = MaterialTheme.typography.bodyLarge,
                color = MindVaultColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun WelcomeCard(onAddConceptClick: () -> Unit) {
    MindVaultCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MindVaultColors.SageLight,
    ) {
        Text(
            text = "Welcome to MindVault",
            style = MaterialTheme.typography.titleLarge,
            color = MindVaultColors.TextPrimary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))
        Text(
            text = "Save a word or concept once, add your own notes, and find it again whenever you need it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextPrimary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space4))
        PrimaryButton(text = "Add a concept", onClick = onAddConceptClick, icon = Icons.Filled.Add)
    }
}

@Composable
private fun QuickActions(
    onAddConceptClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onVaultClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
        ) {
            QuickActionCard(
                label = "Add concept",
                icon = Icons.Outlined.Edit,
                accent = MindVaultColors.AccentTerracotta,
                onClick = onAddConceptClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            QuickActionCard(
                label = "Search",
                icon = Icons.Outlined.Search,
                accent = MindVaultColors.AccentBlue,
                onClick = onSearchClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
        ) {
            QuickActionCard(
                label = "Flashcards",
                icon = Icons.Outlined.Style,
                accent = MindVaultColors.AccentLilac,
                onClick = onFlashcardsClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            QuickActionCard(
                label = "My Vault",
                icon = Icons.Outlined.Bookmarks,
                accent = MindVaultColors.AccentGold,
                onClick = onVaultClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    accent: AccentColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MindVaultCard(modifier = modifier, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ShapeMedium)
                .background(accent.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent.foreground, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Text(label, style = MaterialTheme.typography.titleMedium, color = MindVaultColors.TextPrimary)
    }
}