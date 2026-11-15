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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.MealLogCard
import com.fitai.app.ui.screens.nutrition.photo.rememberMealPhotoPicker
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.NutritionViewModel
import com.fitai.app.viewmodel.nutrition.NutritionEffect
import com.fitai.app.viewmodel.nutrition.NutritionIntent
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NutritionScreen(
    onNavigateMealLog: () -> Unit,
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val picker = rememberMealPhotoPicker(
        onPicked = { bytes, mimeType -> viewModel.onIntent(NutritionIntent.PhotoPicked(bytes, mimeType)) },
        onCancelled = { viewModel.onIntent(NutritionIntent.PhotoPickCancelled) },
    )

    LaunchedEffect(viewModel) {
        viewModel.onIntent(NutritionIntent.RefreshAccess)
        viewModel.effects.collect { effect ->
            when (effect) {
                NutritionEffect.NavigateToMealLog -> onNavigateMealLog()
                NutritionEffect.NavigateToPaywall -> onNavigatePaywall()
                NutritionEffect.MealSaved -> Unit
            }
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
        Text(
            text = strings.nutritionTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FitAIColors.OnSurface,
        )
        val today = state.today
        if (state.activePlan == null) {
            Text(strings.nutritionNoPlan, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurfaceMuted)
        } else if (today != null) {
            RemainingMacrosCard(overview = today)
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
            text = strings.nutritionLogMeal,
            onClick = { viewModel.onIntent(NutritionIntent.OpenManualLog) },
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Secondary,
        )
        if (state.todayMeals.isNotEmpty()) {
            Text(strings.nutritionTodayMeals, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
            state.todayMeals.forEach { meal ->
                MealLogCard(
                    mealName = meal.mealName,
                    calories = meal.calories,
                    macrosLine = strings.nutritionMealMacros(
                        meal.proteinG.roundToInt(),
                        meal.carbsG.roundToInt(),
                        meal.fatG.roundToInt(),
                    ),
                )
            }
        }
        FitAIButton(
            text = strings.nutritionRegenerate,
            onClick = { viewModel.onIntent(NutritionIntent.RegeneratePlan) },
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Ghost,
            enabled = !state.generating,
        )
    }
}
