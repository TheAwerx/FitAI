package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitai.app.access.FeatureAccess
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.NextMealSuggestion
import com.fitai.app.nutrition.TodayNutritionOverview
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAIProGateCard
import com.fitai.app.ui.components.FitAIQuotaChip
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.nutrition.MealFormFields
import com.fitai.app.viewmodel.nutrition.NextMealState
import com.fitai.app.viewmodel.nutrition.PlatePhotoState
import com.fitai.app.viewmodel.nutrition.ReviewError
import kotlin.math.roundToInt

@Composable
internal fun RemainingMacrosCard(
    overview: TodayNutritionOverview,
    modifier: Modifier = Modifier,
) {
    val targets = overview.targets ?: return
    val remaining = overview.remaining ?: return
    val consumed = overview.consumed
    val strings = LocalStrings.current
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.nutritionRemainingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.OnSurface,
                )
                Text(
                    text = if (targets.isTrainingDay) strings.nutritionTrainingDayTarget else strings.nutritionRestDayTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.Primary,
                    modifier = Modifier
                        .background(FitAIColors.Primary.copy(alpha = 0.12f), FitAIShapes.chip)
                        .padding(horizontal = FitAISpacing.sm, vertical = FitAISpacing.xs),
                )
            }
            Text(
                text = strings.nutritionKcalLeft(remaining.calories),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = FitAIColors.OnSurface,
            )
            MacroProgressRow(
                label = strings.nutritionCalories,
                value = strings.nutritionKcalProgress(consumed.calories, targets.calories),
                consumed = consumed.calories.toDouble(),
                target = targets.calories.toDouble(),
            )
            MacroProgressRow(
                label = strings.nutritionProtein,
                value = strings.nutritionGramsProgress(consumed.proteinG.roundToInt(), targets.proteinG),
                consumed = consumed.proteinG,
                target = targets.proteinG.toDouble(),
            )
            MacroProgressRow(
                label = strings.nutritionCarbs,
                value = strings.nutritionGramsProgress(consumed.carbsG.roundToInt(), targets.carbsG),
                consumed = consumed.carbsG,
                target = targets.carbsG.toDouble(),
            )
            MacroProgressRow(
                label = strings.nutritionFat,
                value = strings.nutritionGramsProgress(consumed.fatG.roundToInt(), targets.fatG),
                consumed = consumed.fatG,
                target = targets.fatG.toDouble(),
            )
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    value: String,
    consumed: Double,
    target: Double,
) {
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    val over = target > 0 && consumed > target * 1.05
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
            Text(value, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(FitAIColors.SurfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (over) FitAIColors.Warning else FitAIColors.Primary),
            )
        }
    }
}

/** "Tabak → Plan" region: picker entry, analyzing, editable review, or the quota gate. */
@Composable
internal fun PlatePhotoCard(
    plate: PlatePhotoState,
    access: FeatureAccess?,
    isCameraAvailable: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onReviewChange: (MealFormFields) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    when (plate) {
        PlatePhotoState.Analyzing -> FitAICard(modifier = modifier.fillMaxWidth()) {
            ProgressLine(text = strings.nutritionAnalyzing)
        }
        is PlatePhotoState.Review -> FitAICard(modifier = modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                    Text(strings.nutritionReviewTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                    Text(strings.nutritionReviewHint, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                }
                MealMacroFields(fields = plate.fields, onChange = onReviewChange, enabled = !plate.saving)
                plate.error?.let { error ->
                    Text(
                        text = if (error == ReviewError.Invalid) strings.validationFieldsRequired else strings.errorUnknown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitAIColors.Error,
                    )
                }
                FitAIButton(
                    text = strings.actionSave,
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !plate.saving,
                )
                FitAIButton(
                    text = strings.actionCancel,
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                    variant = FitAIButtonVariant.Ghost,
                    enabled = !plate.saving,
                )
            }
        }
        PlatePhotoState.Idle, PlatePhotoState.Failed -> if (access != null && !access.allowed) {
            FitAIProGateCard(
                title = strings.nutritionPlateLockedTitle,
                body = strings.quotaExhausted,
                ctaLabel = strings.upgradeToPro,
                onClick = onPaywall,
                modifier = modifier,
                badgeLabel = strings.proBadge,
            )
        } else {
            FitAICard(modifier = modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                        Text(strings.nutritionPlateTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                        Text(strings.nutritionPlateBody, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                    }
                    access?.remaining?.let { remaining ->
                        FitAIQuotaChip(label = strings.quotaRemaining(remaining, access.feature.freeQuota))
                    }
                    if (plate == PlatePhotoState.Failed) {
                        Text(strings.nutritionAnalysisFailed, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Error)
                    }
                    FitAIButton(
                        text = if (isCameraAvailable) strings.nutritionPlateCta else strings.nutritionPlateGallery,
                        onClick = if (isCameraAvailable) onCamera else onGallery,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isCameraAvailable) {
                        FitAIButton(
                            text = strings.nutritionPlateGallery,
                            onClick = onGallery,
                            modifier = Modifier.fillMaxWidth(),
                            variant = FitAIButtonVariant.Ghost,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NextMealCard(
    state: NextMealState,
    onRetry: () -> Unit,
    onPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    when (state) {
        NextMealState.Hidden -> Unit
        NextMealState.Locked -> FitAIProGateCard(
            title = strings.nutritionNextMealTitle,
            body = strings.nutritionNextMealLockedBody,
            ctaLabel = strings.upgradeToPro,
            onClick = onPaywall,
            modifier = modifier,
            badgeLabel = strings.proBadge,
        )
        NextMealState.Loading -> NextMealContainer(modifier) {
            ProgressLine(text = strings.nutritionNextMealLoading)
        }
        NextMealState.Failed -> NextMealContainer(modifier) {
            Text(strings.nutritionNextMealError, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Error)
            FitAIButton(
                text = strings.actionRetry,
                onClick = onRetry,
                variant = FitAIButtonVariant.Secondary,
            )
        }
        is NextMealState.Ready -> NextMealContainer(modifier) {
            NextMealContent(state.suggestion)
        }
    }
}

@Composable
private fun NextMealContainer(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Text(
                text = LocalStrings.current.nutritionNextMealTitle,
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
            )
            content()
        }
    }
}

@Composable
private fun NextMealContent(suggestion: NextMealSuggestion) {
    val strings = LocalStrings.current
    Text(suggestion.title, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
    if (suggestion.description.isNotBlank()) {
        Text(suggestion.description, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
    }
    suggestion.items.forEach { item ->
        Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Box(
                modifier = Modifier
                    .padding(top = FitAISpacing.sm)
                    .size(6.dp)
                    .background(FitAIColors.Primary, CircleShape),
            )
            Text(item, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
        }
    }
    Text(
        text = strings.nutritionNextMealMacros(
            suggestion.calories,
            suggestion.proteinG.roundToInt(),
            suggestion.carbsG.roundToInt(),
            suggestion.fatG.roundToInt(),
        ),
        style = MaterialTheme.typography.labelSmall,
        color = FitAIColors.Primary,
    )
}

@Composable
private fun ProgressLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = FitAIColors.Primary,
            trackColor = FitAIColors.SurfaceVariant,
            strokeWidth = 3.dp,
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
    }
}
