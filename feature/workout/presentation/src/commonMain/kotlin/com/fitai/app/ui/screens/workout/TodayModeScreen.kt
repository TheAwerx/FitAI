package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.fitai.app.access.FeatureAccess
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.model.TodayLocation
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAIProGateCard
import com.fitai.app.ui.components.FitAIQuotaChip
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.TodayModeViewModel
import com.fitai.app.viewmodel.workout.TodayModeIntent
import com.fitai.app.viewmodel.workout.TodayModeState
import com.fitai.app.viewmodel.workout.TodayPainArea
import com.fitai.app.viewmodel.workout.WorkoutEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayModeScreen(
    onBack: () -> Unit,
    onNavigateActiveSession: (sessionId: String) -> Unit,
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayModeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(Unit) { viewModel.onIntent(TodayModeIntent.Load) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.NavigateToActiveSession -> onNavigateActiveSession(effect.sessionId)
                WorkoutEffect.NavigateToPaywall -> onNavigatePaywall()
                else -> Unit
            }
        }
    }

    val access = state.access
    val locked = access != null && !access.allowed
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .padding(horizontal = FitAISpacing.lg),
    ) {
        WorkoutTopBar(title = strings.workoutTodayModeTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Text(strings.workoutTodayModeSubtitle, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurfaceMuted)
                access?.let { QuotaChip(it, strings) }
            }
            when {
                locked -> FitAIProGateCard(
                    title = strings.featureLockedTitle,
                    body = strings.quotaExhausted,
                    ctaLabel = strings.upgradeToPro,
                    onClick = onNavigatePaywall,
                    badgeLabel = strings.proBadge,
                )
                access == null && !state.checkingAccess -> ErrorBlock(
                    onRetry = { viewModel.onIntent(TodayModeIntent.Load) },
                )
                else -> TodayModeForm(state = state, onIntent = viewModel::onIntent)
            }
            if (state.errorMessage != null && access != null) {
                Text(strings.errorUnknown, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Error)
            }
        }
        if (!locked && access != null) {
            FitAIButton(
                text = if (state.submitting) strings.workoutTodayAdapting else strings.workoutTodaySubmit,
                onClick = { viewModel.onIntent(TodayModeIntent.Submit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FitAISpacing.md),
                enabled = !state.submitting,
                trailingIcon = if (state.submitting) null else FitAIIcons.ArrowForward,
            )
        }
    }
}

@Composable
private fun QuotaChip(access: FeatureAccess, strings: Strings) {
    val remaining = access.remaining ?: return
    FitAIQuotaChip(
        label = if (remaining > 0) strings.quotaRemaining(remaining, access.feature.freeQuota) else strings.quotaExhausted,
        exhausted = remaining == 0,
    )
}

@Composable
private fun ErrorBlock(onRetry: () -> Unit) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Text(strings.errorUnknown, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Error)
        FitAIButton(text = strings.actionRetry, onClick = onRetry, variant = FitAIButtonVariant.Secondary)
    }
}

@Composable
private fun TodayModeForm(
    state: TodayModeState,
    onIntent: (TodayModeIntent) -> Unit,
) {
    val strings = LocalStrings.current
    val enabled = !state.submitting
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg)) {
        Section(strings.workoutTodayMinutesLabel) {
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                TodayModeState.MINUTE_OPTIONS.forEach { minutes ->
                    WorkoutChip(
                        label = strings.workoutMinutes(minutes),
                        selected = state.minutes == minutes,
                        onClick = { onIntent(TodayModeIntent.SelectMinutes(minutes)) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                }
            }
        }
        Section(strings.workoutTodayEnergyLabel) {
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                (1..5).forEach { level ->
                    WorkoutChip(
                        label = level.toString(),
                        selected = state.energy == level,
                        onClick = { onIntent(TodayModeIntent.SelectEnergy(level)) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    strings.workoutTodayEnergyLow,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
                Text(strings.workoutTodayEnergyHigh, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            }
        }
        Section(strings.workoutTodayPainLabel) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
            ) {
                TodayPainArea.entries.forEach { area ->
                    WorkoutChip(
                        label = painLabel(area, strings),
                        selected = area in state.painAreas,
                        onClick = { onIntent(TodayModeIntent.TogglePain(area)) },
                        enabled = enabled,
                    )
                }
                WorkoutChip(
                    label = strings.workoutPainNone,
                    selected = state.painAreas.isEmpty(),
                    onClick = { onIntent(TodayModeIntent.ClearPain) },
                    enabled = enabled,
                )
            }
        }
        Section(strings.workoutTodayLocationLabel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FitAIColors.SurfaceVariant, FitAIShapes.chip)
                    .padding(FitAISpacing.xs),
            ) {
                TodayModeState.LOCATIONS.forEach { location ->
                    val selected = state.location == location
                    WorkoutChip(
                        label = locationLabel(location, strings),
                        selected = selected,
                        onClick = { onIntent(TodayModeIntent.SelectLocation(location)) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        horizontalPadding = FitAISpacing.xs,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        WorkoutSectionLabel(title)
        content()
    }
}

private fun painLabel(area: TodayPainArea, strings: Strings): String = when (area) {
    TodayPainArea.Knee -> strings.workoutPainKnee
    TodayPainArea.LowerBack -> strings.workoutPainLowerBack
    TodayPainArea.Shoulder -> strings.workoutPainShoulder
    TodayPainArea.Wrist -> strings.workoutPainWrist
}

private fun locationLabel(location: String, strings: Strings): String = when (location) {
    TodayLocation.HOME -> strings.workoutLocationHome
    TodayLocation.HOTEL -> strings.workoutLocationHotel
    TodayLocation.OUTDOOR -> strings.workoutLocationOutdoor
    else -> strings.workoutGym
}
