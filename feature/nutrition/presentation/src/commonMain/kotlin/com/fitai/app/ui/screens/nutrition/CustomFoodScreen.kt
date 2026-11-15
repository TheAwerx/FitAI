package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.MealSlot
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.CustomFoodViewModel
import com.fitai.app.viewmodel.nutrition.CustomFoodEffect
import com.fitai.app.viewmodel.nutrition.CustomFoodIntent
import org.koin.compose.viewmodel.koinViewModel

/** A user food entered per serving; saved, then opened in the serving picker to add it. */
@Composable
fun CustomFoodScreen(
    slot: MealSlot,
    onBack: () -> Unit,
    onSaved: (foodId: String, slot: MealSlot) -> Unit,
    modifier: Modifier = Modifier,
    session: Int = 0,
    viewModel: CustomFoodViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(slot, session) { viewModel.onIntent(CustomFoodIntent.Load(slot, session)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CustomFoodEffect.Saved -> onSaved(effect.foodId, effect.slot)
            }
        }
    }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val perServing = if (state.servingUnit == FoodUnits.GRAM) strings.foodPortion("100", FoodUnits.GRAM, 100) else strings.foodUnitLabel(state.servingUnit, 1.0)
    Column(modifier = modifier.fillMaxSize().background(FitAIColors.Background)) {
        NutritionTopBar(strings.customFoodTitle, onBack, Modifier.padding(horizontal = FitAISpacing.lg))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            FitAITextField(
                value = state.name,
                onValueChange = { viewModel.onIntent(CustomFoodIntent.NameChanged(it)) },
                label = strings.customFoodName,
                enabled = !state.saving,
            )
            SectionLabel(strings.customFoodServingUnit)
            ChipRow(
                items = FoodUnits.customServing,
                selected = state.servingUnit,
                label = { strings.foodUnitLabel(it, 1.0) },
                onSelect = { viewModel.onIntent(CustomFoodIntent.UnitSelected(it)) },
                enabled = !state.saving,
            )
            if (state.servingUnit != FoodUnits.GRAM) {
                FitAITextField(
                    value = state.servingGrams,
                    onValueChange = { viewModel.onIntent(CustomFoodIntent.GramsChanged(it)) },
                    label = strings.customFoodServingGrams,
                    enabled = !state.saving,
                    keyboardOptions = decimal,
                )
            }
            SectionLabel("${strings.customFoodPerServing} ($perServing)")
            FitAITextField(
                value = state.calories,
                onValueChange = { viewModel.onIntent(CustomFoodIntent.CaloriesChanged(it)) },
                label = strings.nutritionCalories,
                enabled = !state.saving,
                keyboardOptions = decimal,
            )
            FitAITextField(
                value = state.proteinG,
                onValueChange = { viewModel.onIntent(CustomFoodIntent.ProteinChanged(it)) },
                label = strings.nutritionProtein,
                enabled = !state.saving,
                keyboardOptions = decimal,
            )
            FitAITextField(
                value = state.carbsG,
                onValueChange = { viewModel.onIntent(CustomFoodIntent.CarbsChanged(it)) },
                label = strings.nutritionCarbs,
                enabled = !state.saving,
                keyboardOptions = decimal,
            )
            FitAITextField(
                value = state.fatG,
                onValueChange = { viewModel.onIntent(CustomFoodIntent.FatChanged(it)) },
                label = strings.nutritionFat,
                enabled = !state.saving,
                keyboardOptions = decimal,
            )
            if (state.invalid || state.failed) {
                Text(
                    text = if (state.invalid) strings.foodFieldsInvalid else strings.errorUnknown,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.Error,
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md)) {
            FitAIButton(
                text = strings.customFoodSave,
                onClick = { viewModel.onIntent(CustomFoodIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )
        }
    }
}
