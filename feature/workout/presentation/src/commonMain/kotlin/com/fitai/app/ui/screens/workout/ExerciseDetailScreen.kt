package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.ExerciseDetailViewModel
import com.fitai.app.viewmodel.workout.ExerciseDetailIntent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onStartSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    Column(
        modifier = modifier.fillMaxSize().padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        FitAIButton(
            text = strings.actionBack,
            onClick = onBack,
            variant = FitAIButtonVariant.Secondary,
        )
        val exercise = state.exercise
        if (exercise != null) {
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
            Text(exercise.muscleGroup, style = MaterialTheme.typography.bodyMedium)
            Text(
                strings.workoutSetsReps(exercise.sets, exercise.reps),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                strings.workoutRest(exercise.restSeconds),
                style = MaterialTheme.typography.bodyMedium,
            )
            exercise.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
            }
            FitAIButton(
                text = strings.workoutReplace,
                onClick = { viewModel.onIntent(ExerciseDetailIntent.OpenReplace) },
                modifier = Modifier.fillMaxWidth(),
                variant = FitAIButtonVariant.Secondary,
            )
        }
    }

    state.replaceSheet?.let { sheet ->
        ExerciseAlternativesSheet(
            sheet = sheet,
            onSelect = { viewModel.onIntent(ExerciseDetailIntent.ReplaceWith(it)) },
            onDismiss = { viewModel.onIntent(ExerciseDetailIntent.DismissReplace) },
        )
    }
}
