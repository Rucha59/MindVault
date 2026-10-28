package com.mindvault.feature.flashcards

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultFilterChip
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.core.ui.components.SecondaryButton
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.StudySession

@Composable
fun StudyScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = when (val current = state) {
        is StudyUiState.Active -> current.deckName
        is StudyUiState.Empty -> current.deckName
        else -> "Study"
    }

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = title, onBackClick = onBackClick)

        when (val current = state) {
            StudyUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            StudyUiState.Error -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't open this deck",
                    message = "Go back and try again.",
                )
            }

            is StudyUiState.Empty -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.Style,
                    title = "No cards to study",
                    message = "Add cards to this deck first.",
                    actionLabel = "Back to deck",
                    onAction = onBackClick,
                )
            }

            is StudyUiState.Active -> if (current.session.isFinished) {
                FinishedContent(
                    session = current.session,
                    onRestart = viewModel::onRestart,
                    onPrevious = viewModel::onPrevious,
                    onBackToDeck = onBackClick,
                )
            } else {
                CardContent(
                    session = current.session,
                    message = current.message,
                    onFlip = viewModel::onFlip,
                    onNext = viewModel::onNext,
                    onPrevious = viewModel::onPrevious,
                    onToggleShuffle = viewModel::onToggleShuffle,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CardContent(
    session: StudySession,
    message: String?,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
) {
    val card = session.current ?: return
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Card ${session.position} of ${session.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MindVaultColors.TextSecondary,
            )
            // The check mark on the selected chip means shuffle state isn't shown by color alone.
            MindVaultFilterChip(label = "Shuffle", selected = session.isShuffled, onClick = onToggleShuffle)
        }
        Spacer(Modifier.height(MindVaultSpacing.space2))
        LinearProgressIndicator(
            progress = { session.position.toFloat() / session.total },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(ShapePill),
            color = MindVaultColors.Forest,
            trackColor = MindVaultColors.SurfaceVariant,
        )
        Spacer(Modifier.height(MindVaultSpacing.space4))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            FlashcardFace(item = card.knowledgeItem, revealed = session.isRevealed, onFlip = onFlip)
        }

        message?.let {
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(MindVaultSpacing.space3))
        PrimaryButton(
            text = if (session.isRevealed) "Hide answer" else "Show answer",
            onClick = onFlip,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))
        Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            SecondaryButton(
                text = "Previous",
                onClick = onPrevious,
                enabled = session.hasPrevious,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = if (session.isLastCard) "Finish" else "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(MindVaultSpacing.space4))
    }
}

/** Term on the front; definition, notes and example on the back. Tapping the card flips it. */
@Composable
private fun FlashcardFace(item: KnowledgeItem, revealed: Boolean, onFlip: () -> Unit) {
    MindVaultCard(
        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
        onClick = onFlip,
        containerColor = if (revealed) MindVaultColors.SageLight else MindVaultColors.Surface,
    ) {
        Crossfade(targetState = revealed, animationSpec = tween(150), label = "cardFace") { showBack ->
            if (showBack) BackFace(item) else FrontFace(item)
        }
    }
}

@Composable
private fun FrontFace(item: KnowledgeItem) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 248.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Term",
            style = MaterialTheme.typography.labelLarge,
            color = MindVaultColors.TextSecondary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MindVaultColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MindVaultSpacing.space5))
        Text(
            text = "Tap the card or choose Show answer",
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BackFace(item: KnowledgeItem) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 248.dp),
        verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space4),
    ) {
        BackSection(label = "Definition", body = item.definition)
        item.personalNotes.takeIf { it.isNotBlank() }?.let { BackSection(label = "Personal notes", body = it) }
        item.example?.takeIf { it.isNotBlank() }?.let { BackSection(label = "Example", body = it) }
    }
}

@Composable
private fun BackSection(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MindVaultColors.TextSecondary)
        Text(text = body, style = MaterialTheme.typography.bodyLarge, color = MindVaultColors.TextPrimary)
    }
}

@Composable
private fun ColumnScope.FinishedContent(
    session: StudySession,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onBackToDeck: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.CheckCircle,
            title = if (session.isDeckComplete) "Deck complete" else "Session complete",
            message = "You reviewed ${session.reviewedThisRun.size} of ${cardsLabel(session.total)} this session. " +
                    "${session.reviewedInDeck} of ${session.total} in this deck have been reviewed.",
            actionLabel = "Study again",
            onAction = onRestart,
        )
        SecondaryButton(text = "Back to deck", onClick = onBackToDeck)
        Spacer(Modifier.height(MindVaultSpacing.space2))
        SecondaryButton(text = "Previous card", onClick = onPrevious)
        Spacer(Modifier.height(MindVaultSpacing.space6))
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