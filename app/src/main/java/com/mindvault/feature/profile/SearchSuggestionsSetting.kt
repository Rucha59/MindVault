package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.SectionHeader

/**
 * The "Search" block of Profile: autocomplete suggestions on or off, and clearing the recent searches.
 * Self-contained: to hide the setting, delete or comment out the single `SearchSuggestionsSetting()` call in
 * ProfileScreen. Suggestions then stay on (the default) and recent searches stay on this device.
 */
@Composable
fun SearchSuggestionsSetting(
    modifier: Modifier = Modifier,
    viewModel: SearchSuggestionsViewModel = hiltViewModel(),
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val hasRecent by viewModel.hasRecentSearches.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()

    SectionHeader("Search")
    MindVaultCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MindVaultSpacing.touchTargetMin)
                .toggleable(value = enabled, role = Role.Switch, onValueChange = viewModel::onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    text = "Suggest words and concepts while you type. Your typing is sent to Wiktionary and Wikipedia for this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = null, // the whole row is the switch, so it is one target for screen readers
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MindVaultColors.TextOnPrimary,
                    checkedTrackColor = MindVaultColors.Forest,
                    uncheckedThumbColor = MindVaultColors.TextSecondary,
                    uncheckedTrackColor = MindVaultColors.SurfaceVariant,
                    uncheckedBorderColor = MindVaultColors.Border,
                ),
            )
        }
        if (hasRecent) {
            TextButton(
                onClick = viewModel::onClearRecentSearches,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Clear recent searches") }
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