package com.mindvault.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.ShapeMedium

/**
 * Labeled input per DESIGN_SYSTEM.md 5.5: Surface fill, Border outline, Forest when focused.
 * The label stays visible while the field has text. [errorText] shows inline and takes
 * priority over [supportingText].
 */
@Composable
fun MindVaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val message = errorText ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorText != null,
        supportingText = message?.let { text -> { Text(text) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = ShapeMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MindVaultColors.TextPrimary,
            unfocusedTextColor = MindVaultColors.TextPrimary,
            focusedContainerColor = MindVaultColors.Surface,
            unfocusedContainerColor = MindVaultColors.Surface,
            errorContainerColor = MindVaultColors.Surface,
            focusedBorderColor = MindVaultColors.Forest,
            unfocusedBorderColor = MindVaultColors.Border,
            focusedLabelColor = MindVaultColors.Forest,
            unfocusedLabelColor = MindVaultColors.TextSecondary,
            cursorColor = MindVaultColors.Forest,
        ),
    )
}