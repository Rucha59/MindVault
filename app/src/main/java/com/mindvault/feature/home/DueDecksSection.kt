package com.mindvault.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.model.DeckDue
import com.mindvault.feature.flashcards.cardsLabel

/** Decks with cards to review today. Tapping a row opens the deck, where the review starts. */
@Composable
internal fun DueDecksSection(
    decks: List<DeckDue>,
    onDeckClick: (deckId: String) -> Unit,
) {
    SectionHeader("Due for review")
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
        decks.forEach { due ->
            MindVaultCard(modifier = Modifier.fillMaxWidth(), onClick = { onDeckClick(due.deck.id) }) {
                Text(
                    text = due.deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    text = "${cardsLabel(due.cardsToReview)} to review",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
    }
}