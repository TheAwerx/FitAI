package com.fitai.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fitai.composeapp.generated.resources.Res
import fitai.composeapp.generated.resources.lexend_medium
import fitai.composeapp.generated.resources.lexend_regular
import fitai.composeapp.generated.resources.lexend_semibold
import fitai.composeapp.generated.resources.sora_bold
import fitai.composeapp.generated.resources.sora_semibold
import org.jetbrains.compose.resources.Font

private val FitAIColorScheme: ColorScheme = darkColorScheme(
    primary = FitAIColors.Primary,
    onPrimary = FitAIColors.OnPrimary,
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
    val sora = FontFamily(
        Font(Res.font.sora_semibold, FontWeight.SemiBold),
        Font(Res.font.sora_bold, FontWeight.Bold),
    )
    val lexend = FontFamily(
        Font(Res.font.lexend_regular, FontWeight.Normal),
        Font(Res.font.lexend_medium, FontWeight.Medium),
        Font(Res.font.lexend_semibold, FontWeight.SemiBold),
    )
    val typography = remember(sora, lexend) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-1).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.8).sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = lexend,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = lexend,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = lexend,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = lexend,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = lexend,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
        )
    }
    MaterialTheme(
        colorScheme = FitAIColorScheme,
        typography = typography,
        shapes = FitAIShapesMaterial,
        content = content,
    )
}
