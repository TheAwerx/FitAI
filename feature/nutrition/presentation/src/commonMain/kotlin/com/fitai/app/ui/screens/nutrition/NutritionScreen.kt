package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.MealSlot
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.screens.nutrition.photo.rememberMealPhotoPicker
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.NutritionViewModel
import com.fitai.app.viewmodel.nutrition.NutritionEffect
import com.fitai.app.viewmodel.nutrition.NutritionIntent
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** The Nutrition tab: today's summary, the meal-by-meal diary with the plan, plate photo and next meal. */
@Composable
fun NutritionScreen(
    onNavigateMealLog: (slot: MealSlot?, mealId: String?) -> Unit,
    onNavigateFoodSearch: (MealSlot) -> Unit,
    onNavigateFoodDetail: (foodId: String?, slot: MealSlot, mealId: String?) -> Unit,
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    var notice by remember { mutableStateOf<String?>(null) }
    val picker = rememberMealPhotoPicker(
        onPicked = { bytes, mimeType -> viewModel.onIntent(NutritionIntent.PhotoPicked(bytes, mimeType)) },
        onCancelled = { viewModel.onIntent(NutritionIntent.PhotoPickCancelled) },
    )

    LaunchedEffect(viewModel) {
        viewModel.onIntent(NutritionIntent.RefreshAccess)
        viewModel.effects.collect { effect ->
            when (effect) {
                is NutritionEffect.NavigateToMealLog -> onNavigateMealLog(effect.slot, effect.mealId)
                is NutritionEffect.NavigateToFoodSearch -> onNavigateFoodSearch(effect.slot)
                is NutritionEffect.NavigateToFoodDetail -> onNavigateFoodDetail(effect.foodId, effect.slot, effect.mealId)
                NutritionEffect.NavigateToPaywall -> onNavigatePaywall()
                NutritionEffect.PlannedMealLogged -> notice = strings.nutritionPlanLogged
                NutritionEffect.MealSaved -> Unit
            }
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MS)
            notice = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
            Text(
                text = strings.nutritionTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = FitAIColors.OnSurface,
            )
            Text(strings.nutritionDiarySubtitle, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
        }
        val plan = state.activePlan
        val today = state.today
        if (plan == null) {
            Text(strings.nutritionNoPlan, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurfaceMuted)
        } else if (today != null) {
            DailySummaryCard(overview = today)
        }
        val totals = state.planTotals
        if (plan != null && totals != null) {
            PlanTotalsCard(totals, plan.dailyCalories, plan.proteinG, plan.carbsG, plan.fatG)
        } else if (plan != null) {
            FitAICard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.nutritionPlanLegacy, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
            }
        }
        notice?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FitAIColors.Primary.copy(alpha = 0.12f), FitAIShapes.button)
                    .padding(FitAISpacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = FitAIColors.Primary,
            )
        }
        state.diary.forEach { slot ->
            MealSlotCard(
                slot = slot,
                planExpanded = slot.entries.isEmpty() != (slot.slot.code in state.toggledPlans),
                busy = state.busySlot == slot.slot.code,
                onAdd = { viewModel.onIntent(NutritionIntent.AddFood(slot.slot)) },
                onOpenEntry = { viewModel.onIntent(NutritionIntent.OpenEntry(it)) },
                onDeleteEntry = { viewModel.onIntent(NutritionIntent.DeleteEntry(it.id)) },
                onLogPlanned = { viewModel.onIntent(NutritionIntent.LogPlanned(slot.slot)) },
                onSwap = { index -> viewModel.onIntent(NutritionIntent.SwapOption(slot.slot, index)) },
                onTogglePlan = { viewModel.onIntent(NutritionIntent.TogglePlan(slot.slot)) },
            )
        }
        PlatePhotoCard(
            plate = state.plate,
            access = state.photoAccess,
            isCameraAvailable = picker.isCameraAvailable,
            onCamera = picker.launchCamera,
            onGallery = picker.launchGallery,
            onReviewChange = { viewModel.onIntent(NutritionIntent.ReviewChanged(it)) },
            onSave = { viewModel.onIntent(NutritionIntent.SaveAnalyzedMeal) },
            onDiscard = { viewModel.onIntent(NutritionIntent.DiscardAnalysis) },
            onPaywall = { viewModel.onIntent(NutritionIntent.OpenPaywall) },
        )
        NextMealCard(
            state = state.nextMeal,
            onRetry = { viewModel.onIntent(NutritionIntent.RetryNextMeal) },
            onPaywall = { viewModel.onIntent(NutritionIntent.OpenPaywall) },
        )
        FitAIButton(
            text = strings.foodQuickAdd,
            onClick = { viewModel.onIntent(NutritionIntent.OpenManualLog) },
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Secondary,
        )
        FitAIButton(
            text = strings.nutritionRegenerate,
            onClick = { viewModel.onIntent(NutritionIntent.RegeneratePlan) },
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Ghost,
            enabled = !state.generating,
        )
    }
}

private const val NOTICE_MS = 2500L
