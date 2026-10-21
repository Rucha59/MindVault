package com.mindvault.feature.knowledge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.mindvault.core.ui.components.MindVaultTextField
import com.mindvault.domain.model.Category

@Composable
internal fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "No category"

    Box(modifier) {
        MindVaultTextField(
            value = selectedName,
            onValueChange = {},
            label = "Category (optional)",
            readOnly = true,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        // Read-only text fields don't reliably forward taps, so a transparent layer opens the menu.
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = "Choose category") { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No category") },
                trailingIcon = if (selectedCategoryId == null) {
                    ({ Icon(Icons.Filled.Check, contentDescription = "Selected") })
                } else null,
                onClick = {
                    expanded = false
                    onCategorySelected(null)
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    trailingIcon = if (category.id == selectedCategoryId) {
                        ({ Icon(Icons.Filled.Check, contentDescription = "Selected") })
                    } else null,
                    onClick = {
                        expanded = false
                        onCategorySelected(category.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("New category…") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreateNewClick()
                },
            )
        }
    }
}