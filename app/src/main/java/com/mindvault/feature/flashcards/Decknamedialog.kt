package com.mindvault.feature.flashcards

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.ui.components.MindVaultTextField

/** Create or rename a deck. The caller owns [error] and [isSaving] and closes the dialog on success. */
@Composable
fun DeckNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    error: String?,
    isSaving: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MindVaultColors.Surface,
        title = { Text(title, color = MindVaultColors.TextPrimary) },
        text = {
            MindVaultTextField(
                value = name,
                onValueChange = { name = it },
                label = "Deck name",
                singleLine = true,
                errorText = error,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (!isSaving) onConfirm(name) }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text(if (isSaving) "Saving…" else confirmLabel) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
            ) { Text("Cancel") }
        },
    )
}