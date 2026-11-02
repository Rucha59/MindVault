package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.model.ThemePreference

/** Profile and settings, reached from the Home top bar. Only settings that actually work are shown. */
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Profile", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            SectionHeader("Appearance")
            MindVaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ThemeOption(
                        label = "System default",
                        supporting = "Match your device setting",
                        selected = state.themePreference == ThemePreference.SYSTEM,
                        onClick = { viewModel.onThemeSelected(ThemePreference.SYSTEM) },
                    )
                    ThemeOption(
                        label = "Light",
                        supporting = null,
                        selected = state.themePreference == ThemePreference.LIGHT,
                        onClick = { viewModel.onThemeSelected(ThemePreference.LIGHT) },
                    )
                    ThemeOption(
                        label = "Dark",
                        supporting = null,
                        selected = state.themePreference == ThemePreference.DARK,
                        onClick = { viewModel.onThemeSelected(ThemePreference.DARK) },
                    )
                }
            }
            if (state.saveFailed) {
                Text(
                    text = "Couldn't save your choice. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = MindVaultSpacing.space3),
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = MindVaultSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // the whole row handles the click
            colors = RadioButtonDefaults.colors(
                selectedColor = MindVaultColors.Forest,
                unselectedColor = MindVaultColors.TextSecondary,
            ),
        )
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MindVaultColors.TextPrimary)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
    }
}