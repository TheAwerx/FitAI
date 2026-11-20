package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.Exercise
import com.fitai.app.model.SessionSet
import com.fitai.app.training.OverloadReason
import com.fitai.app.training.SetSuggestion
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.ActiveSessionViewModel
import com.fitai.app.viewmodel.workout.ActiveSessionIntent
import com.fitai.app.viewmodel.workout.RestTimerState
import com.fitai.app.viewmodel.workout.SetInput
import com.fitai.app.viewmodel.workout.WorkoutEffect
import com.fitai.app.viewmodel.workout.formatKg
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ActiveSessionScreen(
    sessionId: String,
    onBack: () -> Unit,
    onCompleted: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveSessionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is WorkoutEffect.SessionCompleted) onCompleted(effect.sessionId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FitAISpacing.lg),
    ) {
        val plan = state.plan
        WorkoutTopBar(
            title = plan?.takeIf { it.isAdHoc }?.title ?: strings.workoutSessionActive,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            if (plan != null && plan.isAdHoc && plan.description.isNotBlank()) {
                CoachNoteCard(note = plan.description)
            }
            state.sessionExercises.forEach { sessionExercise ->
                val sets = state.setsBySessionExercise[sessionExercise.id].orEmpty()
                ExerciseCard(
                    exercise = state.exerciseDetails[sessionExercise.exerciseId],
                    sets = sets,
                    inputs = state.inputs,
                    suggestion = state.suggestions[sessionExercise.id],
                    canReplace = sets.none { it.completed },
                    onReplace = { viewModel.onIntent(ActiveSessionIntent.OpenReplace(sessionExercise.id)) },
                    onIntent = viewModel::onIntent,
                )
            }
            Spacer(Modifier.height(FitAISpacing.sm))
        }
        Column(
            modifier = Modifier.padding(vertical = FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            state.rest?.let { rest ->
                RestTimerBar(
                    rest = rest,
                    onAdd = { viewModel.onIntent(ActiveSessionIntent.AddRest(15)) },
                    onSkip = { viewModel.onIntent(ActiveSessionIntent.SkipRest) },
                )
            }
            FitAIButton(
                text = strings.workoutCompleteSession,
                onClick = { viewModel.onIntent(ActiveSessionIntent.FinishSession) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.completing && state.session != null,
                trailingIcon = FitAIIcons.ArrowForward,
            )
        }
    }

    state.replaceSheet?.let { sheet ->
        ExerciseAlternativesSheet(
            sheet = sheet,
            onSelect = { viewModel.onIntent(ActiveSessionIntent.ReplaceWith(it)) },
            onDismiss = { viewModel.onIntent(ActiveSessionIntent.DismissReplace) },
        )
    }
}

@Composable
private fun CoachNoteCard(note: String) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            WorkoutIconWell(icon = FitAIIcons.TipsAndUpdates)
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(strings.workoutTodayNoteTitle, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise?,
    sets: List<SessionSet>,
    inputs: Map<String, SetInput>,
    suggestion: SetSuggestion?,
    canReplace: Boolean,
    onReplace: () -> Unit,
    onIntent: (ActiveSessionIntent) -> Unit,
) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                    Text(exercise?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                    if (exercise != null) {
                        Text(
                            "${strings.workoutSetsReps(exercise.sets, exercise.reps)} · ${strings.workoutRest(exercise.restSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FitAIColors.OnSurfaceMuted,
                        )
                    }
                }
                if (canReplace && exercise != null) {
                    TextButton(onClick = onReplace) {
                        Text(strings.workoutReplace, style = MaterialTheme.typography.labelLarge, color = FitAIColors.OnSurfaceMuted)
                    }
                }
            }
            if (suggestion != null && suggestion.reason != OverloadReason.NoHistory) {
                SuggestionHint(suggestion)
            }
            if (sets.isNotEmpty()) SetColumnHeader()
            sets.forEach { set ->
                SetRow(
                    set = set,
                    input = inputs[set.id] ?: SetInput(),
                    canCopyPrevious = !set.completed && sets.any { it.setNumber < set.setNumber },
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun SuggestionHint(suggestion: SetSuggestion) {
    val strings = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FitAIIcons.TipsAndUpdates, contentDescription = null, tint = FitAIColors.Accent, modifier = Modifier.size(18.dp))
        Column {
            Text(
                text = suggestion.weightKg?.let { strings.workoutSuggestedWeight(formatKg(it), suggestion.reps) }
                    ?: strings.workoutSuggestedReps(suggestion.reps),
                style = MaterialTheme.typography.bodyMedium,
                color = FitAIColors.OnSurface,
            )
            Text(
                text = when (suggestion.reason) {
                    OverloadReason.IncreaseWeight -> strings.workoutReasonIncreaseWeight
                    OverloadReason.AddRep -> strings.workoutReasonAddRep
                    OverloadReason.Deload -> strings.workoutReasonDeload
                    OverloadReason.Bodyweight -> strings.workoutReasonBodyweight
                    OverloadReason.NoHistory -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
    }
}

/** Fixed leading/trailing column widths shared by [SetColumnHeader] and [SetRow] so fields line up. */
private val SetCheckColumn = 48.dp
private val SetLabelColumn = 48.dp
private val SetActionColumn = 48.dp

/** One "Reps / Weight (kg)" header per exercise instead of a label inside every field (they wrapped). */
@Composable
private fun SetColumnHeader() {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        Spacer(Modifier.width(SetCheckColumn + SetLabelColumn + FitAISpacing.sm))
        Text(
            strings.workoutActualReps,
            style = MaterialTheme.typography.labelMedium,
            color = FitAIColors.OnSurfaceMuted,
            modifier = Modifier.weight(1f).padding(start = FitAISpacing.xs),
            maxLines = 1,
        )
        Text(
            strings.workoutWeightKg,
            style = MaterialTheme.typography.labelMedium,
            color = FitAIColors.OnSurfaceMuted,
            modifier = Modifier.weight(1f).padding(start = FitAISpacing.xs),
            maxLines = 1,
        )
        Spacer(Modifier.width(SetActionColumn))
    }
}

@Composable
private fun SetRow(
    set: SessionSet,
    input: SetInput,
    canCopyPrevious: Boolean,
    onIntent: (ActiveSessionIntent) -> Unit,
) {
    val strings = LocalStrings.current
    val setLabel = strings.workoutSetNumber(set.setNumber)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        Checkbox(
            checked = set.completed,
            onCheckedChange = { checked -> if (checked) onIntent(ActiveSessionIntent.CompleteSet(set.id)) },
            enabled = !set.completed,
            modifier = Modifier.size(SetCheckColumn),
            colors = CheckboxDefaults.colors(
                checkedColor = FitAIColors.Primary,
                checkmarkColor = FitAIColors.OnPrimary,
                uncheckedColor = FitAIColors.OnSurfaceMuted,
                disabledCheckedColor = FitAIColors.Primary.copy(alpha = 0.6f),
            ),
        )
        Text(
            setLabel,
            style = MaterialTheme.typography.labelLarge,
            color = if (set.completed) FitAIColors.OnSurfaceMuted else FitAIColors.OnSurface,
            modifier = Modifier.width(SetLabelColumn),
            maxLines = 1,
        )
        FitAITextField(
            value = input.reps,
            onValueChange = { onIntent(ActiveSessionIntent.UpdateReps(set.id, it)) },
            label = null,
            placeholder = set.targetReps,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$setLabel, ${strings.workoutActualReps}" },
            enabled = !set.completed,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FitAITextField(
            value = input.weight,
            onValueChange = { onIntent(ActiveSessionIntent.UpdateWeight(set.id, it)) },
            label = null,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$setLabel, ${strings.workoutWeightKg}" },
            enabled = !set.completed,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        if (canCopyPrevious) {
            IconButton(
                onClick = { onIntent(ActiveSessionIntent.CopyPreviousSet(set.id)) },
                modifier = Modifier.size(SetActionColumn),
            ) {
                Icon(
                    FitAIIcons.ContentCopy,
                    contentDescription = strings.workoutCopyPreviousSet,
                    tint = FitAIColors.OnSurfaceMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(SetActionColumn))
        }
    }
}

@Composable
private fun RestTimerBar(
    rest: RestTimerState,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.workoutRestTitle, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                    Text(
                        text = formatClock(rest.remainingSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = FitAIColors.OnSurface,
                    )
                }
                FitAIButton(text = strings.workoutRestAdd15, onClick = onAdd, variant = FitAIButtonVariant.Secondary)
                FitAIButton(text = strings.workoutRestSkip, onClick = onSkip, variant = FitAIButtonVariant.Ghost)
            }
            LinearProgressIndicator(
                progress = { rest.remainingSeconds.toFloat() / rest.totalSeconds.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = FitAIColors.Primary,
                trackColor = FitAIColors.SurfaceVariant,
                strokeCap = StrokeCap.Butt,
                drawStopIndicator = {},
            )
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
