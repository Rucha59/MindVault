package com.mindvault.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
)

/** Type scale from DESIGN_SYSTEM.md section 3. Styles not listed there keep Material defaults. */
val MindVaultTypography = Typography(
        displaySmall = style(32, 38, FontWeight.SemiBold),
        headlineSmall = style(24, 30, FontWeight.SemiBold),
        titleLarge = style(20, 26, FontWeight.SemiBold),
        titleMedium = style(16, 22, FontWeight.SemiBold),
        bodyLarge = style(16, 24, FontWeight.Normal),
        bodyMedium = style(14, 20, FontWeight.Normal),
        bodySmall = style(12, 16, FontWeight.Normal),
        labelLarge = style(14, 20, FontWeight.Medium),
        labelMedium = style(12, 16, FontWeight.Medium),
        labelSmall = style(11, 14, FontWeight.Medium),
)