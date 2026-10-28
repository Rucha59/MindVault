package com.mindvault.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.domain.model.DeckSummary

/** Card counts, a progress bar and a "Deck complete" marker (an icon plus text, not color alone). */
@Composable
internal fun DeckProgress(summary: DeckSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
        Text(
            text = summary.countsLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
        )
        if (summary.totalCards > 0) {
            LinearProgressIndicator(
                progress = { summary.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(ShapePill),
                color = MindVaultColors.Forest,
                trackColor = MindVaultColors.SurfaceVariant,
            )
        }
        if (summary.isComplete) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MindVaultColors.Forest,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MindVaultSpacing.space2))
                Text(
                    text = "Deck complete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MindVaultColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
internal fun DeckTag(text: String) {
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

internal fun cardsLabel(count: Int): String = if (count == 1) "1 card" else "$count cards"

internal fun DeckSummary.countsLabel(): String =
    if (totalCards == 0) "No cards yet" else "${cardsLabel(totalCards)} · $reviewedCards reviewed"