package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.ActivityLevel
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FocusArea
import com.fitai.app.model.Gender
import com.fitai.app.model.SessionLength
import com.fitai.app.model.TrainingExperience
import com.fitai.app.model.TrainingObstacle
import com.fitai.app.preferences.AttributionSources
import com.fitai.app.ui.components.FitAINumericPicker
import com.fitai.app.ui.components.FitAISegmentedControl
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.onboarding.OnboardingIntent
import com.fitai.app.viewmodel.onboarding.OnboardingState

@Composable
internal fun StepGoal(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingGoalTitle, subtitle = strings.onboardingGoalSubtitle) {
        FitnessGoal.entries.forEach { goal ->
            SelectionCard(
                title = strings.goalLabel(goal.code),
                subtitle = strings.goalDescription(goal.code),
                selected = state.goal == goal.code,
                onClick = { onIntent(OnboardingIntent.GoalSelected(goal.code)) },
                icon = goalIcon(goal),
            )
        }
    }
}

@Composable
internal fun StepObstacle(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingObstacleTitle) {
        TrainingObstacle.entries.forEach { obstacle ->
            SelectionCard(
                title = strings.obstacleLabel(obstacle.code),
                selected = state.obstacle == obstacle.code,
                onClick = { onIntent(OnboardingIntent.ObstacleSelected(obstacle.code)) },
                icon = obstacleIcon(obstacle),
            )
        }
    }
}

@Composable
internal fun StepFocus(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingFocusTitle, subtitle = strings.onboardingFocusSubtitle) {
        FocusArea.entries.forEach { area ->
            val selected = area.code in state.focusAreas
            SelectionCard(
                title = strings.focusLabel(area.code),
                subtitle = if (selected) strings.focusDescription(area.code) else null,
                selected = selected,
                onClick = { onIntent(OnboardingIntent.FocusToggled(area.code)) },
                icon = focusIcon(area),
                multiSelect = true,
            )
        }
        if (state.focusLimitReached) {
            Text(
                text = strings.onboardingFocusSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = FitAIColors.Warning,
            )
        }
    }
}

@Composable
internal fun StepAttribution(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingAttributionTitle, subtitle = strings.onboardingAttributionSubtitle) {
        AttributionSources.all.forEach { source ->
            SelectionCard(
                title = strings.attributionLabel(source),
                selected = state.attributionSource == source,
                onClick = { onIntent(OnboardingIntent.AttributionSelected(source)) },
            )
        }
    }
}

@Composable
internal fun StepExperience(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingExperienceTitle) {
        TrainingExperience.entries.forEachIndexed { index, level ->
            val selected = state.experience == level.code
            SelectionCard(
                title = strings.experienceLabel(level.code),
                selected = selected,
                onClick = { onIntent(OnboardingIntent.ExperienceSelected(level.code)) },
                leading = { SignalBars(level = index, selected = selected) },
            )
        }
    }
}

@Composable
internal fun StepGender(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    val selected = Gender.fromCode(state.gender)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = strings.onboardingGenderTitle, subtitle = strings.onboardingGenderSubtitle)
        Spacer(Modifier.height(FitAISpacing.xxl))
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(FitAIColors.Primary.copy(alpha = if (selected == null) 0.05f else 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
            ) { gender ->
                Icon(
                    imageVector = if (gender == Gender.Female) FitAIIcons.Female else FitAIIcons.Male,
                    contentDescription = null,
                    tint = if (gender == null) FitAIColors.OnSurfaceMuted.copy(alpha = 0.4f) else FitAIColors.Primary,
                    modifier = Modifier.size(140.dp),
                )
            }
        }
        Spacer(Modifier.height(FitAISpacing.xxl))
        FitAISegmentedControl(
            options = Gender.entries.toList(),
            selected = selected ?: Gender.Male,
            label = { strings.genderLabel(it.code) },
            onSelect = { onIntent(OnboardingIntent.GenderSelected(it.code)) },
            modifier = Modifier.fillMaxWidth(),
            highlightSelected = selected != null,
        )
    }
}

@Composable
internal fun StepName(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.xl),
    ) {
        StepHeader(strings.onboardingNameTitle, strings.onboardingNameSubtitle)
        OnboardingGlassTextField(
            value = state.name,
            onValueChange = { onIntent(OnboardingIntent.NameChanged(it)) },
            label = strings.onboardingLabelName,
            placeholder = strings.onboardingNamePlaceholder,
        )
    }
}

@Composable
internal fun StepAge(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    val age = state.age.toIntOrNull() ?: 25
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = strings.onboardingAgeTitle, subtitle = strings.onboardingAgeSubtitle)
        Spacer(Modifier.height(FitAISpacing.xxl))
        FitAINumericPicker(
            value = age.coerceIn(13, 80),
            onValueChange = { onIntent(OnboardingIntent.AgeChanged(it.toString())) },
            range = 13..80,
            unit = strings.onboardingAgeUnit,
        )
    }
}

@Composable
internal fun StepHeight(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    val height = state.heightCm.toIntOrNull() ?: 175
    var metric by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = strings.onboardingHeightTitle, subtitle = strings.onboardingHeightSubtitle)
        Spacer(Modifier.height(FitAISpacing.xl))
        FitAISegmentedControl(
            options = listOf(true, false),
            selected = metric,
            label = { if (it) strings.onboardingHeightUnitCm else strings.onboardingHeightUnitFt },
            onSelect = { metric = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FitAISpacing.xl))
        FitAINumericPicker(
            value = height.coerceIn(140, 210),
            onValueChange = { onIntent(OnboardingIntent.HeightChanged(it.toString())) },
            range = 140..210,
            unit = if (metric) strings.onboardingHeightUnitCm else "",
            format = { cm -> if (metric) cm.toString() else cmToFeetInches(cm) },
        )
    }
}

private fun cmToFeetInches(cm: Int): String {
    val totalInches = (cm / 2.54).toInt()
    return "${totalInches / 12}'${totalInches % 12}\""
}

@Composable
internal fun StepWeight(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    val tenths = ((state.weightKg.toDoubleOrNull() ?: 75.5) * 10).toInt().coerceIn(400, 1800)
    val weightValues = remember { (400..1800 step 5).toList() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = strings.onboardingWeightTitle, subtitle = strings.onboardingWeightSubtitle)
        Spacer(Modifier.height(FitAISpacing.xxl))
        FitAINumericPicker(
            value = tenths,
            onValueChange = { onIntent(OnboardingIntent.WeightChanged((it / 10.0).toString())) },
            range = 400..1800,
            values = weightValues,
            unit = strings.onboardingWeightUnit,
            format = { (it / 10.0).toString() },
        )
        Spacer(Modifier.height(FitAISpacing.xxl))
        InfoNoteCard(
            rows = listOf(
                FitAIIcons.VerifiedUser to strings.onboardingWeightPrivacy,
                FitAIIcons.AutoAwesome to strings.onboardingWeightUsage,
            ),
        )
    }
}

@Composable
internal fun StepActivity(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingActivityTitle, subtitle = strings.onboardingActivitySubtitle) {
        ActivityLevel.entries.forEach { level ->
            SelectionCard(
                title = strings.activityLabel(level.code),
                subtitle = strings.activityDescription(level.code),
                selected = state.activityLevel == level.code,
                onClick = { onIntent(OnboardingIntent.ActivitySelected(level.code)) },
                icon = activityIcon(level),
            )
        }
    }
}

@Composable
internal fun StepEquipment(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(title = strings.onboardingEquipmentTitle, subtitle = strings.onboardingEquipmentSubtitle)
        Spacer(Modifier.height(FitAISpacing.lg))
        val tiles = listOf(
            EquipmentOption.FullGym to FitAIIcons.Domain,
            EquipmentOption.Dumbbells to FitAIIcons.FitnessCenter,
            EquipmentOption.Barbell to FitAIIcons.Trophy,
            EquipmentOption.ResistanceBands to FitAIIcons.Straighten,
            EquipmentOption.PullUpBar to FitAIIcons.HorizontalRule,
            EquipmentOption.Bodyweight to FitAIIcons.DirectionsRun,
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                row.forEach { (option, icon) ->
                    EquipmentTile(
                        title = strings.equipmentLabel(option.code),
                        selected = option.code in state.equipment,
                        icon = icon,
                        onClick = { onIntent(OnboardingIntent.EquipmentToggled(option.code)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(FitAISpacing.md))
        }
    }
}

@Composable
private fun EquipmentTile(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = FitAIShapes.card,
        color = if (selected) FitAIColors.Primary.copy(alpha = 0.05f) else FitAIColors.Surface,
        border = BorderStroke(2.dp, if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(FitAISpacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurface,
                modifier = Modifier.align(Alignment.TopStart).padding(end = FitAISpacing.lg),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) FitAIColors.Primary else FitAIColors.OnSurfaceMuted,
                modifier = Modifier.align(Alignment.BottomEnd).size(44.dp),
            )
            if (selected) {
                Icon(
                    imageVector = FitAIIcons.CheckCircle,
                    contentDescription = null,
                    tint = FitAIColors.Primary,
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun StepDays(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingDaysTitle) {
        OnboardingState.DAY_OPTIONS.forEach { days ->
            SelectionCard(
                title = strings.onboardingDaysLabel(days),
                trailingText = strings.onboardingDaysTag(days),
                selected = state.daysPerWeek == days,
                onClick = { onIntent(OnboardingIntent.DaysSelected(days)) },
                showRadio = false,
            )
        }
    }
}

@Composable
internal fun StepDuration(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    OptionList(title = strings.onboardingDurationTitle) {
        SessionLength.options.forEach { minutes ->
            SelectionCard(
                title = strings.onboardingDurationLabel(minutes),
                trailingText = strings.onboardingDurationTag(minutes),
                selected = state.sessionMinutes == minutes,
                onClick = { onIntent(OnboardingIntent.DurationSelected(minutes)) },
                showRadio = false,
            )
        }
    }
}

@Composable
internal fun StepDiet(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(title = strings.onboardingDietTitle, subtitle = strings.onboardingDietSubtitle)
        Spacer(Modifier.height(FitAISpacing.lg))
        DietRestriction.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                row.forEach { option ->
                    DietChip(
                        title = strings.dietLabel(option.code),
                        selected = option.code in state.dietRestrictions,
                        onClick = { onIntent(OnboardingIntent.DietToggled(option.code)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(FitAISpacing.sm))
        }
    }
}

@Composable
private fun DietChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = FitAIShapes.button,
        color = if (selected) FitAIColors.Primary else FitAIColors.Surface,
        border = BorderStroke(2.dp, if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) FitAIColors.OnPrimary else FitAIColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(FitAIIcons.CheckCircle, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Start-aligned title block followed by a vertical list of options. */
@Composable
private fun OptionList(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        StepHeader(title = title, subtitle = subtitle)
        Spacer(Modifier.height(FitAISpacing.xs))
        content()
    }
}

@Composable
internal fun ValidationText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun goalIcon(goal: FitnessGoal): ImageVector = when (goal) {
    FitnessGoal.LoseFat -> FitAIIcons.LocalFireDepartment
    FitnessGoal.BuildMuscle -> FitAIIcons.FitnessCenter
    FitnessGoal.GetStronger -> FitAIIcons.Trophy
    FitnessGoal.ImproveEndurance -> FitAIIcons.DirectionsRun
    FitnessGoal.StayHealthy -> FitAIIcons.Favorite
}

private fun obstacleIcon(obstacle: TrainingObstacle): ImageVector = when (obstacle) {
    TrainingObstacle.Time -> FitAIIcons.Schedule
    TrainingObstacle.Knowledge -> FitAIIcons.MenuBook
    TrainingObstacle.Motivation -> FitAIIcons.TrendingDown
    TrainingObstacle.Injury -> FitAIIcons.Warning
    TrainingObstacle.Equipment -> FitAIIcons.FitnessCenter
    TrainingObstacle.NeverTried -> FitAIIcons.AutoAwesome
}

private fun focusIcon(area: FocusArea): ImageVector = when (area) {
    FocusArea.PersonalPlan -> FitAIIcons.AutoAwesome
    FocusArea.SmartOverload -> FitAIIcons.ShowChart
    FocusArea.PlatePhoto -> FitAIIcons.PhotoCamera
    FocusArea.LogLifts -> FitAIIcons.Edit
    FocusArea.Streak -> FitAIIcons.LocalFireDepartment
    FocusArea.AiCoach -> FitAIIcons.TipsAndUpdates
}

private fun activityIcon(level: ActivityLevel): ImageVector = when (level) {
    ActivityLevel.Sedentary -> FitAIIcons.Weekend
    ActivityLevel.Light -> FitAIIcons.DirectionsWalk
    ActivityLevel.Moderate -> FitAIIcons.FitnessCenter
    ActivityLevel.Very -> FitAIIcons.Sprint
    ActivityLevel.Athlete -> FitAIIcons.Bolt
}
