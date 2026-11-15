package com.fitai.app.ui.screens.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.SupportedLanguages
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.ui.theme.FitAITheme
import fitai.composeapp.generated.resources.Res
import fitai.composeapp.generated.resources.welcome_hero
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onHaveAccount: () -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background),
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            Image(
                painter = painterResource(Res.drawable.welcome_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.45f,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to FitAIColors.Background.copy(alpha = 0.6f),
                                0.55f to FitAIColors.Background.copy(alpha = 0.85f),
                                1.0f to FitAIColors.Background,
                            ),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FitAISpacing.lg)
                .padding(top = FitAISpacing.md, bottom = FitAISpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                LanguageChip(current = strings.languageCode, onSelect = onLanguageChange)
            }
            Spacer(Modifier.height(FitAISpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Icon(FitAIIcons.FitnessCenter, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(36.dp))
                Text(strings.appName, style = MaterialTheme.typography.displayMedium, color = FitAIColors.OnSurface)
            }
            Spacer(Modifier.weight(0.6f))
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xl)) {
                LaurelProof(strings.welcomeProofPlanTitle, strings.welcomeProofPlanBody)
                LaurelProof(strings.welcomeProofCoachTitle, strings.welcomeProofCoachBody)
                LaurelProof(strings.welcomeProofProgressTitle, strings.welcomeProofProgressBody)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = strings.welcomeHeadline,
                style = MaterialTheme.typography.displayMedium,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.xl))
            FitAIButton(
                text = strings.welcomeCta,
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = FitAIShapes.button,
                        ambientColor = FitAIColors.Primary.copy(alpha = 0.3f),
                        spotColor = FitAIColors.Primary.copy(alpha = 0.3f),
                    ),
                variant = FitAIButtonVariant.Primary,
                trailingIcon = FitAIIcons.ArrowForward,
            )
            Spacer(Modifier.height(FitAISpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.welcomeHaveAccountPrefix, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                TextButton(onClick = onHaveAccount) {
                    Text(strings.welcomeSignInLink, style = MaterialTheme.typography.titleMedium, color = FitAIColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(current: String, onSelect: (String) -> Unit) {
    val strings = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            shape = FitAIShapes.chip,
            color = FitAIColors.Surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(start = FitAISpacing.md, end = FitAISpacing.sm, top = FitAISpacing.xs, bottom = FitAISpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(current.uppercase(), style = MaterialTheme.typography.labelLarge, color = FitAIColors.OnSurface)
                Icon(FitAIIcons.ExpandMore, contentDescription = strings.settingsLanguage, tint = FitAIColors.OnSurfaceMuted)
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = FitAIShapes.card,
            containerColor = FitAIColors.Surface,
        ) {
            SupportedLanguages.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            strings.languageName(code),
                            color = if (code == current) FitAIColors.Primary else FitAIColors.OnSurface,
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(code)
                    },
                    trailingIcon = if (code == current) {
                        { Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.Primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun LaurelProof(title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Laurel(mirrored = false)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = FitAISpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = FitAIColors.OnSurface, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted, textAlign = TextAlign.Center)
        }
        Laurel(mirrored = true)
    }
}

/** Half wreath of leaves along an arc; [mirrored] draws the right-hand side. */
@Composable
private fun Laurel(mirrored: Boolean, color: Color = FitAIColors.Primary) {
    Canvas(modifier = Modifier.width(30.dp).height(68.dp)) {
        scale(scaleX = if (mirrored) -1f else 1f, scaleY = 1f, pivot = center) {
            val leaves = 7
            val leafSize = Size(size.width * 0.42f, size.width * 0.22f)
            repeat(leaves) { index ->
                val t = index / (leaves - 1f)
                val y = size.height * (0.94f - t * 0.88f)
                val x = size.width * (0.82f - sin(t * PI).toFloat() * 0.5f)
                val angle = 160f + t * 120f
                rotate(degrees = angle, pivot = Offset(x, y)) {
                    drawOval(
                        color = color.copy(alpha = 0.55f + 0.45f * (1f - t)),
                        topLeft = Offset(x, y - leafSize.height / 2),
                        size = leafSize,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    FitAITheme {
        WelcomeScreen(onContinue = {}, onHaveAccount = {}, onLanguageChange = {})
    }
}
