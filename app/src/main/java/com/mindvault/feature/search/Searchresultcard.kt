package com.mindvault.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge
import com.mindvault.core.ui.components.PrimaryButton

/**
 * One Search result. Built on Surface with design-system tokens because the shared MindVaultCard
 * file hasn't been reviewed; swap it in if you prefer.
 */
@Composable
internal fun SearchResultCard(
    item: SearchResultUi,
    isSaving: Boolean,
    onSave: () -> Unit,
    onEditFirst: () -> Unit,
    onViewSaved: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = item.result
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        color = MindVaultColors.Surface,
        border = BorderStroke(1.dp, MindVaultColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(MindVaultSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
        ) {
            Column {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MindVaultColors.TextPrimary,
                )
                result.pronunciation?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
            }

            result.meanings.forEach { meaning ->
                Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
                    if (meaning.partOfSpeech.isNotBlank()) {
                        Text(
                            text = meaning.partOfSpeech.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MindVaultColors.TextSecondary,
                        )
                    }
                    meaning.definitions.forEach { definition ->
                        Text(
                            text = definition.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MindVaultColors.TextPrimary,
                        )
                    }
                }
            }

            Text(
                text = "Source: ${result.sourceName}",
                style = MaterialTheme.typography.bodySmall,
                color = MindVaultColors.TextSecondary,
            )

            val savedId = item.savedItemId
            if (savedId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MindVaultColors.Forest,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(MindVaultSpacing.space2))
                        Text(
                            text = "Already in your Vault",
                            style = MaterialTheme.typography.labelLarge,
                            color = MindVaultColors.TextPrimary,
                        )
                    }
                    TextButton(
                        onClick = { onViewSaved(savedId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("View") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onEditFirst,
                        enabled = !isSaving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("Edit first") }
                    Spacer(Modifier.width(MindVaultSpacing.space2))
                    PrimaryButton(
                        text = if (isSaving) "Saving…" else "Save",
                        onClick = onSave,
                        enabled = !isSaving,
                    )
                }
            }
        }
    }
}