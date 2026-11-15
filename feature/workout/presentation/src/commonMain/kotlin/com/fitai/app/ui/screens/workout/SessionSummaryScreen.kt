package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.training.PersonalRecord
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.SessionSummaryViewModel
import com.fitai.app.viewmodel.workout.SessionSummaryIntent
import com.fitai.app.viewmodel.workout.formatKg
import com.fitai.app.workout.SessionSummary
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SessionSummaryScreen(
    sessionId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(sessionId) { viewModel.onIntent(SessionSummaryIntent.Load(sessionId)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .padding(horizontal = FitAISpacing.lg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = FitAISpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(FitAIColors.Primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FitAIIcons.CheckCircle, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(48.dp))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    strings.workoutSummaryTitle,
                    style = MaterialTheme.typography.displayMedium,
                    color = FitAIColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    strings.workoutSummarySubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitAIColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
            val summary = state.summary
            when {
                state.loading -> CircularProgressIndicator(color = FitAIColors.Primary)
                summary != null -> {
                    SummaryStats(summary)
                    PersonalRecordsSection(state.personalRecords)
                }
            }
        }
        FitAIButton(
            text = strings.workoutSummaryDone,
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FitAISpacing.md),
        )
    }
}

@Composable
private fun SummaryStats(summary: SessionSummary) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        StatTile(strings.workoutSummaryDuration, strings.workoutMinutes(summary.durationMinutes), Modifier.weight(1f))
        StatTile(
            strings.workoutSummaryVolume,
            strings.workoutKg(formatKg(summary.totalVolumeKg.roundToInt().toDouble())),
            Modifier.weight(1f),
        )
        StatTile(
            strings.workoutSummarySets,
            strings.workoutSummarySetsValue(summary.completedSets, summary.totalSets),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    FitAICard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = FitAIColors.OnSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted, maxLines = 1)
    }
}

@Composable
private fun PersonalRecordsSection(records: List<PersonalRecord>) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        WorkoutSectionLabel(strings.workoutSummaryPrTitle)
        if (records.isEmpty()) {
            Text(strings.workoutSummaryNoPr, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
        }
        records.forEach { record ->
            FitAICard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WorkoutIconWell(
                        icon = FitAIIcons.LocalFireDepartment,
                        tint = FitAIColors.Accent,
                        background = FitAIColors.Accent.copy(alpha = 0.2f),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
                    ) {
                        Text(record.exerciseName, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                        Text(
                            strings.workoutPrSet(formatKg(record.weightKg), record.reps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitAIColors.OnSurface,
                        )
                        Text(
                            strings.workoutPrE1rm(formatKg((record.e1rm * 10).roundToInt() / 10.0)),
                            style = MaterialTheme.typography.labelSmall,
                            color = FitAIColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}
