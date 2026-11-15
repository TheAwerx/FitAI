package com.fitai.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.training.EquipmentContext
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.workout.ReplaceSheetState

@Composable
internal fun WorkoutTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = FitAIIcons.ArrowBack,
                contentDescription = LocalStrings.current.actionBack,
                tint = FitAIColors.Primary,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FitAIColors.OnBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
internal fun WorkoutChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontalPadding: Dp = FitAISpacing.md,
) {
    Box(
        modifier = modifier
            .background(if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant, FitAIShapes.chip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = FitAISpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FitAIColors.OnPrimary else FitAIColors.OnSurface,
            maxLines = 1,
        )
    }
}

@Composable
internal fun WorkoutSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = FitAIColors.OnSurface,
    )
}

@Composable
internal fun WorkoutIconWell(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = FitAIColors.OnSurface,
    background: androidx.compose.ui.graphics.Color = FitAIColors.SurfaceVariant,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseAlternativesSheet(
    sheet: ReplaceSheetState,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = FitAIShapes.bottomSheet,
        containerColor = FitAIColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg)
                .padding(bottom = FitAISpacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(strings.workoutReplaceTitle, style = MaterialTheme.typography.headlineSmall, color = FitAIColors.OnSurface)
                Text(strings.workoutReplaceSubtitle, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
            }
            when {
                sheet.loading || sheet.replacing -> Box(
                    modifier = Modifier.fillMaxWidth().padding(FitAISpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FitAIColors.Primary)
                }
                sheet.alternatives.isEmpty() -> Text(
                    strings.workoutReplaceEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurfaceMuted,
                )
                else -> sheet.alternatives.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FitAIColors.SurfaceVariant, FitAIShapes.button)
                            .clickable { onSelect(option.name) }
                            .padding(FitAISpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                            Text(option.name, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                            Text(
                                text = when (option.equipment) {
                                    EquipmentContext.Gym -> strings.workoutGym
                                    EquipmentContext.Bodyweight -> strings.workoutBodyweight
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = FitAIColors.OnSurfaceMuted,
                            )
                        }
                        Icon(FitAIIcons.ChevronRight, contentDescription = null, tint = FitAIColors.OnSurfaceMuted)
                    }
                }
            }
        }
    }
}
