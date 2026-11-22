package com.mindvault.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing

/**
 * Asked when an edited item leaves a category whose deck holds its card. Tapping outside the dialog cancels the
 * save. "Remember my choice" stores the answer as the Profile setting.
 */
@Composable
internal fun DeckRemovalDialog(
    prompt: DeckRemovalPrompt,
    onChoice: (remove: Boolean, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var rememberChoice by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MindVaultColors.Surface,
        title = { Text("Remove from the “${prompt.deckName}” deck too?", color = MindVaultColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
                Text(
                    text = "“${prompt.itemTitle}” is no longer in this deck's category. Remove its card from the " +
                            "“${prompt.deckName}” deck as well? Its review progress in that deck will be lost.",
                    color = MindVaultColors.TextSecondary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = rememberChoice,
                            onValueChange = { rememberChoice = it },
                            role = Role.Checkbox,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MindVaultColors.Forest),
                    )
                    Spacer(Modifier.width(MindVaultSpacing.space2))
                    Text(
                        text = "Remember my choice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextPrimary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChoice(true, rememberChoice) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Remove from deck") }
        },
        dismissButton = {
            TextButton(
                onClick = { onChoice(false, rememberChoice) },
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Keep in deck") }
        },
    )
}