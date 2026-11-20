package com.fitai.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object FitAIColors {
    val Primary = Color(0xFF46F1C5)
    val PrimaryDark = Color(0xFF00A884)
    val PrimaryLight = Color(0xFF5FFFD4)
    val Background = Color(0xFF0D0D0D)
    val Surface = Color(0xFF1A1A1A)
    val SurfaceVariant = Color(0xFF242424)

    // Tonal surface steps (Material 3 surfaceContainer*). Neutral greys, no hue, so sheets,
    // dialogs and bars never pick up Material's default violet-tinted dark surfaces.
    val SurfaceLowest = Color(0xFF0A0A0A)
    val SurfaceLow = Color(0xFF141414)
    val SurfaceHigh = Color(0xFF202020)
    val SurfaceBright = Color(0xFF2E2E2E)

    /** Selected chips and navigation indicators: a deep mint tint, not the accent orange. */
    val SelectedContainer = Color(0xFF123D34)
    val OnSelectedContainer = Color(0xFFB9F8E6)

    /** Control boundaries (≥3:1 on Background and Surface) and quiet dividers. */
    val Outline = Color(0xFF6B6B6B)
    val OutlineVariant = Color(0xFF2E2E2E)

    val ErrorContainer = Color(0xFF3D1414)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val OnPrimary = Color(0xFF00382B)
    val OnBackground = Color(0xFFE5E2E1)
    val OnSurface = Color(0xFFE0E0E0)
    val OnSurfaceMuted = Color(0xFFBACAC2)
    val Accent = Color(0xFFFF6B35)
    val AccentSecondary = Color(0xFF7B61FF)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFB300)
    val Error = Color(0xFFFF5252)
    val GradientPrimary = listOf(Color(0xFF00D4AA), Color(0xFF00A884))
    val GradientEnergy = listOf(Color(0xFFFF6B35), Color(0xFFFFB300))
    val GradientDark = listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D))
}

object FitAISpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object FitAIShapes {
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(50)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}
