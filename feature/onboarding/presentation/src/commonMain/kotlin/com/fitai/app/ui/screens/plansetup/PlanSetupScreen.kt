package com.fitai.app.ui.screens.plansetup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAIProgressRing
import com.fitai.app.ui.notifications.reminderTexts
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.PlanSetupViewModel
import com.fitai.app.viewmodel.plansetup.PlanSetupEffect
import com.fitai.app.viewmodel.plansetup.PlanSetupIntent
import com.fitai.app.viewmodel.plansetup.PlanSetupTrialNotice
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlanSetupScreen(
    onDone: () -> Unit,
    viewModel: PlanSetupViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(PlanSetupIntent.Start(strings.reminderTexts()))
        viewModel.effects.collect { effect ->
            when (effect) {
                PlanSetupEffect.Done -> onDone()
            }
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = {},
    )

    val progress by animateFloatAsState(state.progress)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .background(Brush.verticalGradient(0f to FitAIColors.Primary.copy(alpha = 0.12f), 0.5f to FitAIColors.Background))
            .statusBarsPadding()
            .padding(horizontal = FitAISpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.failed) {
            Icon(FitAIIcons.Warning, contentDescription = null, tint = FitAIColors.Warning, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(FitAISpacing.md))
            Text(
                strings.planSetupErrorTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.sm))
            Text(
                strings.planSetupErrorBody,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.xl))
            FitAIButton(
                text = strings.actionRetry,
                onClick = { viewModel.onIntent(PlanSetupIntent.Retry) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(FitAISpacing.sm))
            FitAIButton(
                text = strings.planSetupUseStarter,
                onClick = { viewModel.onIntent(PlanSetupIntent.UseStarterPlan) },
                variant = FitAIButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }
        Box(contentAlignment = Alignment.Center) {
            FitAIProgressRing(progress = progress, size = 168.dp, strokeWidth = 10.dp)
            Text(
                strings.percentLabel((progress * 100).toInt()),
                style = MaterialTheme.typography.displayMedium,
                color = FitAIColors.OnSurface,
            )
        }
        Spacer(Modifier.height(FitAISpacing.xl))
        Text(
            text = if (state.personal) strings.planSetupPersonalTitle else strings.planSetupStarterTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = FitAIColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        state.trialNotice?.let { notice ->
            Spacer(Modifier.height(FitAISpacing.sm))
            Text(
                text = when (notice) {
                    PlanSetupTrialNotice.UsedOnDevice -> strings.planSetupTrialUsedOnDevice
                    PlanSetupTrialNotice.UsedByAccount -> strings.planSetupTrialUsedByAccount
                    PlanSetupTrialNotice.Pending -> strings.planSetupTrialPending
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(FitAISpacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            SetupRow(strings.planSetupStepWorkout, done = progress >= 0.5f, active = progress < 0.5f)
            SetupRow(strings.planSetupStepNutrition, done = progress >= 0.85f, active = progress in 0.5f..0.85f)
            SetupRow(strings.planSetupStepReminders, done = progress >= 1f, active = progress > 0.85f && progress < 1f)
        }
    }
}

@Composable
private fun SetupRow(text: String, done: Boolean, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Box(
            modifier = Modifier
                .size(24.dp)
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
            if (done) Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (done || active) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted.copy(alpha = 0.6f),
        )
    }
}
