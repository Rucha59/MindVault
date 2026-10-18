package com.mindvault.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge

/**
 * Standard card: Surface background, 16 dp radius and padding, fine Border outline, low elevation.
 * Pass [containerColor] = SageLight for the single highlight card on a screen.
 */
@Composable
fun MindVaultCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MindVaultColors.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickModifier =
        if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier

    Surface(
        modifier = modifier,
        shape = ShapeLarge,
        color = containerColor,
        border = BorderStroke(1.dp, MindVaultColors.Border),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(MindVaultSpacing.cardPadding),
            content = content,
        )
    }
}