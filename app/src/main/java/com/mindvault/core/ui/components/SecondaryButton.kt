package com.mindvault.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge

/** Supporting action (DESIGN_SYSTEM.md 5.3): SurfaceVariant fill with Forest content. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MindVaultSpacing.touchTargetMin),
        enabled = enabled,
        shape = ShapeLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) MindVaultColors.SageLight else MindVaultColors.SurfaceVariant,
            contentColor = MindVaultColors.Forest,
            disabledContainerColor = MindVaultColors.SurfaceVariant,
            disabledContentColor = MindVaultColors.Disabled,
        ),
        interactionSource = interactionSource,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(MindVaultSpacing.space2))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}