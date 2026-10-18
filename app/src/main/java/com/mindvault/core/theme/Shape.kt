package com.mindvault.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ShapeSmall = RoundedCornerShape(8.dp)
val ShapeMedium = RoundedCornerShape(12.dp)
val ShapeLarge = RoundedCornerShape(16.dp)
val ShapeExtraLarge = RoundedCornerShape(24.dp)
val ShapePill = RoundedCornerShape(percent = 50)

val MindVaultShapes = Shapes(
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = ShapeExtraLarge,
)