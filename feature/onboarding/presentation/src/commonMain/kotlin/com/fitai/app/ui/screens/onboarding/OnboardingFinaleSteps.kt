package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.ChartSeries
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAILineChart
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.onboarding.OnboardingIntent
import com.fitai.app.viewmodel.onboarding.OnboardingState
import kotlinx.coroutines.delay

private val MintGlow = Brush.verticalGradient(
    0f to FitAIColors.Primary.copy(alpha = 0.10f),
    0.45f to FitAIColors.Background,
)

@Composable
internal fun AnalyzingPhase(state: OnboardingState) {
    val strings = LocalStrings.current
    val steps = strings.onboardingAnalyzingSteps
    val activeIndex = (state.analyzingPercent * steps.size / 100).coerceAtMost(steps.lastIndex)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintGlow)
            .statusBarsPadding()
            .padding(horizontal = FitAISpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = strings.percentLabel(state.analyzingPercent),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = FitAIColors.OnSurface,
        )
        Spacer(Modifier.height(FitAISpacing.md))
        Text(strings.onboardingAnalyzingTitle, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
        Spacer(Modifier.height(FitAISpacing.lg))
        OnboardingProgressBar(
            progress = state.analyzingPercent / 100f,
            variant = OnboardingProgressVariant.ThickRounded,
        )
        Spacer(Modifier.height(FitAISpacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            steps.forEachIndexed { index, text ->
                val done = index < activeIndex || state.analyzingPercent >= 100
                val active = index == activeIndex && !done
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                when {
                                    done -> FitAIColors.Primary
                                    active -> FitAIColors.Primary.copy(alpha = 0.2f)
                                    else -> FitAIColors.SurfaceVariant
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            active -> FitAIColors.OnSurface
                            done -> FitAIColors.OnSurfaceMuted
                            else -> FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ResultPhase(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize().background(MintGlow)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = FitAISpacing.lg)
                .padding(top = FitAISpacing.xl, bottom = FitAISpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.onboardingResultTitle(state.name.trim()),
                style = MaterialTheme.typography.displayMedium,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.md))
            Text(
                text = strings.onboardingResultSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            state.goalDate?.let { date ->
                Spacer(Modifier.height(FitAISpacing.md))
                Surface(
                    shape = FitAIShapes.chip,
                    color = FitAIColors.Primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, FitAIColors.Primary.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.sm + FitAISpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                    ) {
                        Icon(FitAIIcons.CalendarToday, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(18.dp))
                        Text(
                            strings.longDate(date.day, date.month, date.year),
                            style = MaterialTheme.typography.titleMedium,
                            color = FitAIColors.OnSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(FitAISpacing.xl))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                strings.onboardingResultBenefits(state.goal).forEachIndexed { index, line ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                        IconWell(
                            icon = listOf(FitAIIcons.FitnessCenter, FitAIIcons.ShowChart, FitAIIcons.LocalFireDepartment)[index % 3],
                            selected = true,
                            size = 40.dp,
                        )
                        Text(line, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                    }
                }
            }
            Spacer(Modifier.height(FitAISpacing.xl))
            Text(strings.onboardingResultChartTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurfaceMuted)
            Spacer(Modifier.height(FitAISpacing.md))
            ComparisonChart()
        }
        OnboardingPrimaryBar(
            text = strings.actionContinue,
            enabled = true,
            onClick = { onIntent(OnboardingIntent.ContinueFromResult) },
        )
    }
}

@Composable
private fun ComparisonChart() {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Box(modifier = Modifier.padding(FitAISpacing.md)) {
            FitAILineChart(
                series = listOf(
                    ChartSeries(listOf(1f, 1.6f, 1.3f, 2.1f, 2.9f, 3.1f, 3.6f, 4.4f), color = FitAIColors.Primary, showEndDot = true),
                    ChartSeries(listOf(1f, 1.5f, 1.05f, 1.55f, 1.7f, 1.65f, 1.75f, 1.7f), color = FitAIColors.Error, fill = false, showEndDot = true),
                ),
                height = 200.dp,
                minValue = 0.6f,
                maxValue = 4.8f,
                dashedLevel = 1.9f,
            )
            Text(
                strings.onboardingResultWithFitAI,
                style = MaterialTheme.typography.labelLarge,
                color = FitAIColors.Primary,
                modifier = Modifier.align(Alignment.TopStart).padding(top = FitAISpacing.lg),
            )
            Text(
                strings.onboardingResultPlateau,
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.Error,
                modifier = Modifier.align(Alignment.CenterStart).padding(top = FitAISpacing.lg),
            )
            Text(
                strings.onboardingResultWithoutFitAI,
                style = MaterialTheme.typography.labelLarge,
                color = FitAIColors.Error,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = FitAISpacing.md, end = FitAISpacing.lg),
            )
        }
    }
}

@Composable
internal fun QuotePhase(onContinue: () -> Unit) {
    val strings = LocalStrings.current
    val lines = strings.onboardingQuoteLines
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(lines) {
        lines.indices.forEach { index ->
            delay(if (index == 0) 300 else 900)
            shown = index + 1
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintGlow)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onContinue)
            .padding(horizontal = FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lines.forEachIndexed { index, line ->
            AnimatedVisibility(
                visible = shown > index,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 },
            ) {
                Text(
                    text = line,
                    style = if (index == 0) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                    fontStyle = FontStyle.Italic,
                    color = if (index == lines.lastIndex) FitAIColors.Primary else FitAIColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun PledgePhase(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintGlow)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(FitAISpacing.xxl))
        Text(
            text = strings.onboardingPledgeLead,
            style = MaterialTheme.typography.headlineLarge,
            color = FitAIColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = strings.onboardingPledgeDays(state.resolvedDays),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = FitAIColors.OnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = FitAISpacing.sm),
        )
        Spacer(Modifier.height(FitAISpacing.xl))
        Text(
            text = strings.onboardingPledgeTagline,
            style = MaterialTheme.typography.headlineMedium,
            color = FitAIColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        state.errorMessage?.let {
            ValidationText(strings.errorUnknown)
            Spacer(Modifier.height(FitAISpacing.md))
        }
        FingerprintHoldButton(
            label = if (state.pledgeSigned) strings.onboardingPledgeSigned else strings.onboardingPledgeHold,
            accessibilityLabel = strings.onboardingCommitmentHoldA11y,
            signed = state.pledgeSigned,
            onConfirm = { onIntent(OnboardingIntent.ConfirmPledge) },
        )
        Spacer(Modifier.height(FitAISpacing.lg))
    }
}

@Composable
internal fun SaveErrorPhase(onRetry: () -> Unit, onBack: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = strings.onboardingErrorTitle, subtitle = strings.onboardingErrorSubtitle, centered = true)
        Spacer(Modifier.height(FitAISpacing.md))
        FitAIButton(text = strings.actionRetry, onClick = onRetry, modifier = Modifier.fillMaxWidth())
        FitAIButton(
            text = strings.actionBack,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Secondary,
        )
    }
}
