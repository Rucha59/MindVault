package com.mindvault.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MindVaultColors.TextPrimary,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MindVaultSpacing.space3)
            .semantics { heading() },
    )
}