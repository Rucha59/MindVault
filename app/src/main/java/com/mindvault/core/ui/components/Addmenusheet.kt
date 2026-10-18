package com.mindvault.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeExtraLarge
import com.mindvault.core.theme.ShapeMedium

/**
 * Modal sheet opened by the centered Add button. Only MVP-supported actions are listed;
 * "Create a flashcard" is intentionally omitted until decks exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuSheet(
    onDismiss: () -> Unit,
    onAddConcept: () -> Unit,
    onSearchAndSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ShapeExtraLarge,
        containerColor = MindVaultColors.Surface,
        scrimColor = MindVaultColors.Scrim,
    ) {
        Text(
            text = "Add",
            style = MaterialTheme.typography.titleLarge,
            color = MindVaultColors.TextPrimary,
            modifier = Modifier
                .padding(horizontal = MindVaultSpacing.screenHorizontal)
                .semantics { heading() },
        )
        Spacer(Modifier.height(MindVaultSpacing.space2))
        AddMenuRow(
            icon = Icons.Outlined.Edit,
            title = "Add a concept",
            supporting = "Enter a word or concept yourself",
            onClick = onAddConcept,
        )
        AddMenuRow(
            icon = Icons.Outlined.Search,
            title = "Search and save",
            supporting = "Open Search",
            onClick = onSearchAndSave,
        )
        Spacer(Modifier.height(MindVaultSpacing.space6))
    }
}

@Composable
private fun AddMenuRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MindVaultSpacing.listItemMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MindVaultSpacing.screenHorizontal, vertical = MindVaultSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ShapeMedium)
                .background(MindVaultColors.SageLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MindVaultColors.Forest, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MindVaultColors.TextPrimary)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
        }
    }
}