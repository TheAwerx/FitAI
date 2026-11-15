package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.MealSlot
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.util.appLocale
import com.fitai.app.viewmodel.FoodDetailViewModel
import com.fitai.app.viewmodel.displayName
import com.fitai.app.viewmodel.nutrition.FoodDetailEffect
import com.fitai.app.viewmodel.nutrition.FoodDetailError
import com.fitai.app.viewmodel.nutrition.FoodDetailIntent
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/** Serving picker with a live macro preview; adds the food to a meal or updates a logged entry. */
@Composable
fun FoodDetailScreen(
    foodId: String?,
    mealId: String?,
    slot: MealSlot,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    session: Int = 0,
    viewModel: FoodDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(foodId, mealId, slot, session) { viewModel.onIntent(FoodDetailIntent.Load(foodId, mealId, slot, session)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FoodDetailEffect.Saved, FoodDetailEffect.Deleted -> onDone()
            }
        }
    }
    val locale = appLocale()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background),
    ) {
        NutritionTopBar(
            title = if (state.isEditing) strings.mealEntryEditTitle else strings.nutritionAddFood,
            onBack = onBack,
            modifier = Modifier.padding(horizontal = FitAISpacing.lg),
        )
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(FitAISpacing.xl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FitAIColors.Primary, trackColor = FitAIColors.SurfaceVariant)
            }
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(
                    state.displayName(locale),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FitAIColors.OnSurface,
                )
                val food = state.food
                val meta = if (food != null) {
                    listOf(
                        strings.foodCategoryLabel(food.category),
                        strings.foodPer100g(food.kcalPer100g.roundToInt()),
                        strings.foodSourceLabel(food.source),
                    ).joinToString(" · ")
                } else {
                    strings.foodSourceLabel(null)
                }
                Text(meta, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            }
            PreviewCard(state.preview)
            if (state.servings.size > 1) {
                SectionLabel(strings.foodServing)
                ChipRow(
                    items = state.servings.map { it.unit },
                    selected = state.unit,
                    label = { unit -> strings.servingLabel(state.servings.first { it.unit == unit }) },
                    onSelect = { viewModel.onIntent(FoodDetailIntent.UnitSelected(it)) },
                    enabled = !state.saving,
                )
            }
            SectionLabel(strings.foodAmount)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                StepButton(FitAIIcons.Remove, enabled = !state.saving) { viewModel.onIntent(FoodDetailIntent.Step(up = false)) }
                FitAITextField(
                    value = state.amountText,
                    onValueChange = { viewModel.onIntent(FoodDetailIntent.AmountChanged(it)) },
                    label = strings.foodUnitLabel(state.unit, state.amountText.replace(',', '.').toDoubleOrNull() ?: 1.0),
                    modifier = Modifier.weight(1f),
                    enabled = !state.saving,
                    isError = state.error == FoodDetailError.InvalidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                StepButton(FitAIIcons.Add, enabled = !state.saving) { viewModel.onIntent(FoodDetailIntent.Step(up = true)) }
            }
            SectionLabel(strings.foodMeal)
            SlotChips(selected = state.slot, onSelect = { viewModel.onIntent(FoodDetailIntent.SlotSelected(it)) }, enabled = !state.saving)
            state.error?.let { error ->
                Text(
                    text = if (error == FoodDetailError.InvalidAmount) strings.foodInvalidAmount else strings.errorUnknown,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.Error,
                )
            }
            if (state.isEditing) {
                FitAIButton(
                    text = strings.foodDeleteEntry,
                    onClick = { viewModel.onIntent(FoodDetailIntent.Delete) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = FitAIButtonVariant.Ghost,
                    enabled = !state.saving,
                    leadingIcon = FitAIIcons.Delete,
                )
            } else if (state.food?.isCustom == true) {
                FitAIButton(
                    text = strings.foodDeleteCustom,
                    onClick = { viewModel.onIntent(FoodDetailIntent.DeleteCustomFood) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = FitAIButtonVariant.Ghost,
                    enabled = !state.saving,
                    leadingIcon = FitAIIcons.Delete,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FitAIColors.Background.copy(alpha = 0.95f))
                .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        ) {
            FitAIButton(
                text = if (state.isEditing) strings.foodUpdateEntry else strings.foodAddToDiary,
                onClick = { viewModel.onIntent(FoodDetailIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving && state.error != FoodDetailError.LoadFailed,
            )
        }
    }
}

@Composable
private fun PreviewCard(preview: com.fitai.app.model.Macros?) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${preview?.calories?.roundToInt() ?: 0}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = FitAIColors.Primary,
                )
                Text(strings.kcalUnit, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            }
            MacroColumn(strings.nutritionProtein, preview?.proteinG)
            MacroColumn(strings.nutritionCarbs, preview?.carbsG)
            MacroColumn(strings.nutritionFat, preview?.fatG)
        }
    }
}

@Composable
private fun MacroColumn(label: String, grams: Double?) {
    Column(modifier = Modifier.padding(start = FitAISpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${((grams ?: 0.0) * 10).roundToInt() / 10.0} g",
            style = MaterialTheme.typography.titleMedium,
            color = FitAIColors.OnSurface,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).background(FitAIColors.SurfaceVariant, androidx.compose.foundation.shape.CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = FitAIColors.Primary)
    }
}
