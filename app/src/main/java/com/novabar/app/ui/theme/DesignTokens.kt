package com.novabar.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape

object DesignTokens {
    // Spacing
    val spacingXs: Dp = 4.dp
    val spacingSm: Dp = 8.dp
    val spacingMd: Dp = 12.dp
    val spacingLg: Dp = 16.dp
    val spacingXl: Dp = 24.dp

    // Corner radius
    val cornerRadiusSmall: Dp = 8.dp
    val cornerRadiusMedium: Dp = 12.dp
    val cornerRadiusLarge: Dp = 16.dp

    // Icon sizes
    val iconSizeSmall: Dp = 20.dp
    val iconSizeMedium: Dp = 24.dp
    val iconSizeLarge: Dp = 32.dp

    // Animation durations (ms)
    const val animationDurationShort = 150
    const val animationDurationMedium = 300
    const val animationDurationLong = 500

    // Elevation
    val elevationCard = 2.dp
    val elevationFab = 6.dp

    // Card shape
    val cardShape = RoundedCornerShape(cornerRadiusMedium)

    // Typography (example)
    val typography = Typography(
        headlineSmall = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
        titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}
