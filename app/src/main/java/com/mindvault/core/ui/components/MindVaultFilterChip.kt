package com.mindvault.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.ShapePill

/** Filter chip per DESIGN_SYSTEM.md 5.6: Forest when selected, with a check mark so color isn't the only cue. */
@Composable
fun MindVaultFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkIcon: (@Composable () -> Unit)? = if (selected) {
        ({
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        })
    } else null

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = checkIcon,
        shape = ShapePill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MindVaultColors.SurfaceVariant,
            labelColor = MindVaultColors.TextPrimary,
            selectedContainerColor = MindVaultColors.Forest,
            selectedLabelColor = MindVaultColors.TextOnPrimary,
            selectedLeadingIconColor = MindVaultColors.TextOnPrimary,
        ),
    )
}