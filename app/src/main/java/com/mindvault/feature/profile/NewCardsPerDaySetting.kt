package com.mindvault.feature.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultFilterChip
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.usecase.ReviewQueueBuilder

/**
 * The "Flashcards" block of Profile: how many new cards each deck introduces per day.
 * Self-contained: to hide the setting, delete or comment out the single `NewCardsPerDaySetting()` call in
 * ProfileScreen. The saved value and the default of 20 keep working without it.
 */
@Composable
fun NewCardsPerDaySetting(
    modifier: Modifier = Modifier,
    viewModel: NewCardsPerDayViewModel = hiltViewModel(),
) {
    val limit by viewModel.limit.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()

    SectionHeader("Flashcards")
    MindVaultCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "New cards per deck per day",
            style = MaterialTheme.typography.titleMedium,
            color = MindVaultColors.TextPrimary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = "How many cards you have not studied yet appear in each deck's daily review.",
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
        ) {
            ReviewQueueBuilder.NEW_CARDS_PER_DAY_OPTIONS.forEach { option ->
                MindVaultFilterChip(
                    label = option.toString(),
                    selected = limit == option,
                    onClick = { viewModel.onSelected(option) },
                )
            }
        }
    }
    if (saveFailed) {
        Text(
            text = "Couldn't save your choice. Please try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = MindVaultSpacing.space3),
        )
    }
}