package com.fitai.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val FitAIColorScheme: ColorScheme = darkColorScheme(
    primary = FitAIColors.Primary,
    onPrimary = FitAIColors.Background,
    primaryContainer = FitAIColors.PrimaryDark,
    secondary = FitAIColors.Accent,
    onSecondary = FitAIColors.Background,
    tertiary = FitAIColors.AccentSecondary,
    onTertiary = FitAIColors.OnBackground,
    background = FitAIColors.Background,
    onBackground = FitAIColors.OnBackground,
    surface = FitAIColors.Surface,
    onSurface = FitAIColors.OnSurface,
    surfaceVariant = FitAIColors.SurfaceVariant,
    onSurfaceVariant = FitAIColors.OnSurfaceMuted,
    error = FitAIColors.Error,
    onError = FitAIColors.OnBackground,
)

private val FitAITypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

private val FitAIShapesMaterial = Shapes(
    extraSmall = FitAIShapes.chip,
    small = FitAIShapes.button,
    medium = FitAIShapes.card,
    large = FitAIShapes.bottomSheet,
)

@Composable
fun FitAITheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FitAIColorScheme,
        typography = FitAITypography,
        shapes = FitAIShapesMaterial,
        content = content,
    )
}
