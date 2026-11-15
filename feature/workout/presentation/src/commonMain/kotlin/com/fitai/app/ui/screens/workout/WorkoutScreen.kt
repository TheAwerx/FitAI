package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitai.app.access.PlanQuota
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.WorkoutPlanCard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.plangeneration.PlanGenerationOverlay
import com.fitai.app.ui.plangeneration.PlanQuotaRow
import com.fitai.app.ui.plangeneration.PlanRegenerateConfirmDialog
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.WorkoutViewModel
import com.fitai.app.viewmodel.workout.WorkoutEffect
import com.fitai.app.viewmodel.workout.WorkoutIntent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkoutScreen(
    onNavigateExercise: (String) -> Unit,
    onNavigateActiveSession: (String) -> Unit,
    onNavigatePaywall: () -> Unit,
    onNavigateTodayMode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(Unit) { viewModel.onIntent(WorkoutIntent.Refresh) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.NavigateToExercise -> onNavigateExercise(effect.exerciseId)
                is WorkoutEffect.NavigateToActiveSession -> onNavigateActiveSession(effect.sessionId)
                WorkoutEffect.NavigateToPaywall -> onNavigatePaywall()
                is WorkoutEffect.SessionCompleted -> Unit
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FitAISpacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Text(strings.workoutTitle, style = MaterialTheme.typography.headlineMedium)
        TodayModeEntryCard(onClick = onNavigateTodayMode)
        val plan = state.activePlan
        if (plan == null) {
            Text(strings.workoutNoPlan)
            FitAIButton(
                text = strings.workoutRegenerate,
                onClick = { viewModel.onIntent(WorkoutIntent.RegeneratePlan) },
                enabled = !state.generating,
                leadingIcon = FitAIIcons.AutoAwesome,
            )
        } else {
            WorkoutPlanCard(
                title = plan.title,
                subtitle = plan.description,
                isActive = true,
                activeLabel = strings.workoutActivePlan,
            )
            RegeneratePlanSection(
                quota = state.planQuota,
                generating = state.generating,
                onRegenerate = { viewModel.onIntent(WorkoutIntent.RegeneratePlan) },
            )
            // Today (e.g. Sunday) can sit past the right edge; keep the selected chip on screen.
            val selectedChip = remember { BringIntoViewRequester() }
            LaunchedEffect(state.selectedDay) {
                withFrameNanos { }
                selectedChip.bringIntoView()
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
            ) {
                (1..7).forEach { day ->
                    val selected = state.selectedDay == day
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onIntent(WorkoutIntent.SelectDay(day)) },
                        label = { Text(strings.weekdayShort(day), maxLines = 1) },
                        modifier = if (selected) Modifier.bringIntoViewRequester(selectedChip) else Modifier,
                    )
                }
            }
            if (state.dayExercises.isEmpty()) {
                Text(
                    text = strings.workoutRestDayHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
            state.dayExercises.forEach { exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateExercise(exercise.id) }
                        .padding(vertical = FitAISpacing.sm),
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        strings.workoutSetsReps(exercise.sets, exercise.reps),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        strings.workoutRest(exercise.restSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FitAIButton(
                text = strings.workoutStartSession,
                onClick = { viewModel.onIntent(WorkoutIntent.StartSession) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.dayExercises.isNotEmpty(),
            )
            if (state.plans.size > 1) {
                Text(strings.workoutHistory, style = MaterialTheme.typography.titleSmall)
                state.plans.filter { !it.isActive }.forEach { old ->
                    WorkoutPlanCard(
                        title = old.title,
                        subtitle = old.description,
                        isActive = false,
                        activeLabel = "",
                    )
                }
            }
        }
    }

    if (state.confirmRegenerate) {
        PlanRegenerateConfirmDialog(
            quota = state.planQuota,
            onConfirm = { viewModel.onIntent(WorkoutIntent.ConfirmRegenerate) },
            onDismiss = { viewModel.onIntent(WorkoutIntent.DismissRegenerate) },
        )
    }
    state.generation?.let { generation ->
        PlanGenerationOverlay(
            state = generation,
            onRetry = { viewModel.onIntent(WorkoutIntent.RetryGeneration) },
            onClose = { viewModel.onIntent(WorkoutIntent.CloseGeneration) },
        )
    }
}

@Composable
private fun RegeneratePlanSection(
    quota: PlanQuota?,
    generating: Boolean,
    onRegenerate: () -> Unit,
) {
    val strings = LocalStrings.current
    val exhausted = quota?.exhausted == true
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        FitAIButton(
            text = strings.workoutRegenerate,
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Secondary,
            enabled = !generating && !exhausted,
            leadingIcon = FitAIIcons.AutoAwesome,
        )
        if (quota != null) PlanQuotaRow(quota = quota)
    }
}

@Composable
private fun TodayModeEntryCard(onClick: () -> Unit) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkoutIconWell(icon = FitAIIcons.TipsAndUpdates)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
            ) {
                Text(strings.workoutTodayModeTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                Text(strings.workoutTodayModeEntryBody, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
            }
            Icon(FitAIIcons.ChevronRight, contentDescription = null, tint = FitAIColors.OnSurfaceMuted)
        }
    }
}
