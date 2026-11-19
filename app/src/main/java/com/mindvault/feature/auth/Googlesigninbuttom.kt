package com.mindvault.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mindvault.R
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge

/**
 * "Continue with Google" button following Google's sign-in branding guidelines: official four-colour
 * "G" logo, fixed neutral light/dark colours, standard label wording. The colours are Google's, not
 * MindVault tokens, on purpose: do not restyle this button. Check the logo against Google's current
 * branding assets before release.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (dark) Color(0xFF131314) else Color(0xFFFFFFFF)
    val border = if (dark) Color(0xFF8E918F) else Color(0xFF747775)
    val content = if (dark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MindVaultSpacing.touchTargetMin),
        shape = ShapeLarge,
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.6f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
    }
}