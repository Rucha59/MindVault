package com.mindvault.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.ShapeLarge
import androidx.compose.foundation.text.KeyboardActions
/** Search input: leading search icon, clear action when non-empty, Forest focus outline. */
@Composable
fun MindVaultSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        singleLine = true,
        shape = ShapeLarge,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (value.isNotEmpty() && enabled) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MindVaultColors.TextPrimary,
            unfocusedTextColor = MindVaultColors.TextPrimary,
            disabledTextColor = MindVaultColors.TextSecondary,
            focusedContainerColor = MindVaultColors.Surface,
            unfocusedContainerColor = MindVaultColors.Surface,
            disabledContainerColor = MindVaultColors.Surface,
            cursorColor = MindVaultColors.Forest,
            focusedBorderColor = MindVaultColors.Forest,
            unfocusedBorderColor = MindVaultColors.Border,
            disabledBorderColor = MindVaultColors.Border,
            focusedLeadingIconColor = MindVaultColors.Forest,
            unfocusedLeadingIconColor = MindVaultColors.TextSecondary,
            disabledLeadingIconColor = MindVaultColors.Disabled,
            focusedTrailingIconColor = MindVaultColors.TextSecondary,
            unfocusedTrailingIconColor = MindVaultColors.TextSecondary,
            focusedPlaceholderColor = MindVaultColors.TextSecondary,
            unfocusedPlaceholderColor = MindVaultColors.TextSecondary,
            disabledPlaceholderColor = MindVaultColors.TextSecondary,
        ),
    )
}