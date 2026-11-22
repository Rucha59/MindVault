package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.domain.model.DeckRemovalPreference

private val options = listOf(
    DeckRemovalPreference.ASK to "Ask me each time",
    DeckRemovalPreference.ALWAYS_REMOVE to "Always remove it from the deck",
    DeckRemovalPreference.ALWAYS_KEEP to "Always keep it in the deck",
)

/**
 * "When I take an item out of a category": what happens to its card in that category's flashcard deck.
 * Self-contained: to hide the setting, delete or comment out the single `CategoryDeckSetting()` call in
 * ProfileScreen. The saved value and the default ("Ask me each time") keep working without it.
 */
@Composable
fun CategoryDeckSetting(
    modifier: Modifier = Modifier,
    viewModel: DeckRemovalViewModel = hiltViewModel(),
) {
    val preference by viewModel.preference.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()

    MindVaultCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "When I take an item out of a category",
            style = MaterialTheme.typography.titleMedium,
            color = MindVaultColors.TextPrimary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = "Choose what happens to its card in that category's flashcard deck.",
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))
        androidx.compose.foundation.layout.Column(Modifier.selectableGroup()) {
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = preference == value,
                            onClick = { viewModel.onSelected(value) },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = preference == value,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MindVaultColors.Forest),
                    )
                    Spacer(Modifier.width(MindVaultSpacing.space2))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextPrimary)
                }
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