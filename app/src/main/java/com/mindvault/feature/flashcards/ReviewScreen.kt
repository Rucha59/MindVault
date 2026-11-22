package com.mindvault.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.core.ui.components.SecondaryButton
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewMode
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewSession

/**
 * Rated review: show the card, reveal the answer, then rate it. Again cards come back once more in the session.
 * Difficult cards that are not due are extra practice: "Still hard" or "Got it", nothing is saved.
 */
@Composable
fun ReviewScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = when (val current = state) {
        is ReviewUiState.Active -> current.mode.title()
        is ReviewUiState.Empty -> current.mode.title()
        else -> "Review"
    }

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = title, onBackClick = onBackClick)

        when (val current = state) {
            ReviewUiState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }

            ReviewUiState.Error -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't start this review",
                    message = "Go back and try again.",
                    actionLabel = "Back to deck",
                    onAction = onBackClick,
                )
            }

            is ReviewUiState.Empty -> MessageBody {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = current.mode.emptyTitle(),
                    message = current.mode.emptyMessage(),
                    actionLabel = "Back to deck",
                    onAction = onBackClick,
                )
            }

            is ReviewUiState.Active -> if (current.session.isFinished) {
                FinishedContent(session = current.session, onBackToDeck = onBackClick)
            } else {
                CardContent(
                    active = current,
                    intervalDays = viewModel::intervalDays,
                    relearnIntervalDays = viewModel::relearnIntervalDays,
                    onFlip = viewModel::onFlip,
                    onRate = viewModel::onRate,
                    onExtraAnswer = viewModel::onExtraAnswer,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CardContent(
    active: ReviewUiState.Active,
    intervalDays: (FlashcardDetail, ReviewRating) -> Int,
    relearnIntervalDays: (ReviewRating) -> Int,
    onFlip: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onExtraAnswer: (gotIt: Boolean) -> Unit,
) {
    val session = active.session
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
                text = active.deckName,
                style = MaterialTheme.typography.labelLarge,
                color = MindVaultColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "${session.cardsLeft} left",
                style = MaterialTheme.typography.labelLarge,
                color = MindVaultColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(MindVaultSpacing.space2))
        LinearProgressIndicator(
            progress = { session.progress },
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

        if (session.isRepeat) {
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Text(
                text = "You marked this one Again. How did it go this time?",
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextSecondary,
            )
        }
        if (session.isExtra) {
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Text(
                text = "Extra practice: this card isn't due, so your schedule won't change.",
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextSecondary,
            )
        }
        active.message?.let {
            Spacer(Modifier.height(MindVaultSpacing.space2))
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(MindVaultSpacing.space3))
        when {
            !session.isRevealed ->
                PrimaryButton(text = "Show answer", onClick = onFlip, modifier = Modifier.fillMaxWidth())

            session.isExtra -> ExtraPracticeButtons(onExtraAnswer = onExtraAnswer)

            else -> RatingButtons(
                card = card,
                isRepeat = session.isRepeat,
                enabled = !active.isSaving,
                intervalDays = intervalDays,
                relearnIntervalDays = relearnIntervalDays,
                onRate = onRate,
            )
        }
        Spacer(Modifier.height(MindVaultSpacing.space4))
    }
}

/** Extra practice: nothing is rated or saved. "Still hard" brings the card back at the end of the session. */
@Composable
private fun ExtraPracticeButtons(onExtraAnswer: (gotIt: Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
        SecondaryButton(
            text = "Still hard",
            onClick = { onExtraAnswer(false) },
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = "Got it",
            onClick = { onExtraAnswer(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Four ratings with the next interval on each button. A card that comes back after Again offers Hard, Good and
 * Easy only: it is shown once more, then leaves the session.
 */
@Composable
private fun RatingButtons(
    card: FlashcardDetail,
    isRepeat: Boolean,
    enabled: Boolean,
    intervalDays: (FlashcardDetail, ReviewRating) -> Int,
    relearnIntervalDays: (ReviewRating) -> Int,
    onRate: (ReviewRating) -> Unit,
) {
    if (isRepeat) {
        fun label(name: String, rating: ReviewRating) = "$name · ${intervalLabel(relearnIntervalDays(rating))}"
        Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            SecondaryButton(
                text = label("Hard", ReviewRating.HARD),
                onClick = { onRate(ReviewRating.HARD) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = label("Good", ReviewRating.GOOD),
                onClick = { onRate(ReviewRating.GOOD) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = label("Easy", ReviewRating.EASY),
                onClick = { onRate(ReviewRating.EASY) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    fun label(name: String, rating: ReviewRating) = "$name · ${intervalLabel(intervalDays(card, rating))}"

    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            SecondaryButton(
                text = label("Again", ReviewRating.AGAIN),
                onClick = { onRate(ReviewRating.AGAIN) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = label("Hard", ReviewRating.HARD),
                onClick = { onRate(ReviewRating.HARD) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            PrimaryButton(
                text = label("Good", ReviewRating.GOOD),
                onClick = { onRate(ReviewRating.GOOD) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = label("Easy", ReviewRating.EASY),
                onClick = { onRate(ReviewRating.EASY) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColumnScope.FinishedContent(session: ReviewSession, onBackToDeck: () -> Unit) {
    val scheduled = if (session.reviewedCount > 0) {
        "You reviewed ${cardsLabel(session.reviewedCount)}.\n" +
                "Again ${session.countOf(ReviewRating.AGAIN)} · Hard ${session.countOf(ReviewRating.HARD)} · " +
                "Good ${session.countOf(ReviewRating.GOOD)} · Easy ${session.countOf(ReviewRating.EASY)}"
    } else {
        null
    }
    val extra = if (session.extraCount > 0) {
        "Extra practice: ${cardsLabel(session.extraCount)}. Your schedule wasn't changed."
    } else {
        null
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.CheckCircle,
            title = "Session complete",
            message = listOfNotNull(scheduled, extra).joinToString("\n"),
            actionLabel = "Back to deck",
            onAction = onBackToDeck,
        )
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

/** "1d", "3d", "2mo", "1y". */
internal fun intervalLabel(days: Int): String = when {
    days < 30 -> "${days}d"
    days < 365 -> "${days / 30}mo"
    else -> "${days / 365}y"
}

private fun ReviewMode.title(): String = when (this) {
    ReviewMode.DAILY -> "Daily review"
    ReviewMode.NEW_CARDS -> "New cards"
    ReviewMode.DIFFICULT -> "Difficult cards"
}

private fun ReviewMode.emptyTitle(): String = when (this) {
    ReviewMode.DAILY -> "All caught up"
    ReviewMode.NEW_CARDS -> "No new cards right now"
    ReviewMode.DIFFICULT -> "No difficult cards"
}

private fun ReviewMode.emptyMessage(): String = when (this) {
    ReviewMode.DAILY -> "No cards are due today. Come back tomorrow, or practice any time from the deck screen."
    ReviewMode.NEW_CARDS -> "Every new card has been started, or today's new-card limit is reached."
    ReviewMode.DIFFICULT -> "Cards you often get wrong will show up here."
}