package com.fitai.app.ui.screens.nutrition

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
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.MealLogViewModel
import com.fitai.app.viewmodel.nutrition.MealFormFields
import com.fitai.app.viewmodel.nutrition.MealLogIntent
import com.fitai.app.viewmodel.nutrition.NutritionEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MealLogScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealLogViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is NutritionEffect.MealSaved) onSaved()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        FitAIButton(
            text = strings.actionBack,
            onClick = onBack,
            variant = FitAIButtonVariant.Secondary,
        )
        Text(strings.nutritionLogMeal, style = MaterialTheme.typography.headlineMedium)
        MealMacroFields(
            fields = MealFormFields(state.mealName, state.calories, state.proteinG, state.carbsG, state.fatG),
            onChange = { fields ->
                with(viewModel) {
                    if (fields.mealName != state.mealName) onIntent(MealLogIntent.MealNameChanged(fields.mealName))
                    if (fields.calories != state.calories) onIntent(MealLogIntent.CaloriesChanged(fields.calories))
                    if (fields.proteinG != state.proteinG) onIntent(MealLogIntent.ProteinChanged(fields.proteinG))
                    if (fields.carbsG != state.carbsG) onIntent(MealLogIntent.CarbsChanged(fields.carbsG))
                    if (fields.fatG != state.fatG) onIntent(MealLogIntent.FatChanged(fields.fatG))
                }
            },
            enabled = !state.saving,
        )
        state.errorMessage?.let {
            Text(strings.validationFieldsRequired, color = MaterialTheme.colorScheme.error)
        }
        FitAIButton(
            text = strings.actionSave,
            onClick = { viewModel.onIntent(MealLogIntent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.saving,
        )
    }
}
