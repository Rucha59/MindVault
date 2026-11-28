package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.feature.stats.StatsViewModel

/**
 * The "Your progress" block of Profile: three real numbers and a way into the full Stats screen.
 * Self-contained: to hide it, delete or comment out the single `ProfileProgressCard(...)` call in ProfileScreen.
 * Numbers show "–" until they are loaded; nothing is invented.
 */
@Composable
fun ProfileProgressCard(
    onViewStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    SectionHeader("Your progress")
    MindVaultCard(modifier = modifier.fillMaxWidth(), onClick = onViewStatsClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
        ) {
            ProgressTile(stats?.totalItems?.toString() ?: "–", "Saved", Modifier.weight(1f))
            ProgressTile(stats?.dueReviews?.toString() ?: "–", "Due today", Modifier.weight(1f))
            ProgressTile(stats?.streakDays?.toString() ?: "–", "Day streak", Modifier.weight(1f))
        }
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "View stats",
                style = MaterialTheme.typography.labelLarge,
                color = MindVaultColors.Forest,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null, // decorative: the whole card is the button
                tint = MindVaultColors.Forest,
            )
        }
    }
}

@Composable
private fun ProgressTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MindVaultColors.Forest,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
        )
    }
}