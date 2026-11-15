package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.nutrition.MealFormFields

/** Meal name + macro inputs shared by manual logging and the photo analysis review. */
@Composable
internal fun MealMacroFields(
    fields: MealFormFields,
    onChange: (MealFormFields) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalStrings.current
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        FitAITextField(
            value = fields.mealName,
            onValueChange = { onChange(fields.copy(mealName = it)) },
            label = strings.nutritionMealName,
            enabled = enabled,
        )
        FitAITextField(
            value = fields.calories,
            onValueChange = { onChange(fields.copy(calories = it)) },
            label = strings.nutritionCalories,
            enabled = enabled,
            keyboardOptions = numeric,
        )
        FitAITextField(
            value = fields.proteinG,
            onValueChange = { onChange(fields.copy(proteinG = it)) },
            label = strings.nutritionProtein,
            enabled = enabled,
            keyboardOptions = decimal,
        )
        FitAITextField(
            value = fields.carbsG,
            onValueChange = { onChange(fields.copy(carbsG = it)) },
            label = strings.nutritionCarbs,
            enabled = enabled,
            keyboardOptions = decimal,
        )
        FitAITextField(
            value = fields.fatG,
            onValueChange = { onChange(fields.copy(fatG = it)) },
            label = strings.nutritionFat,
            enabled = enabled,
            keyboardOptions = decimal,
        )
    }
}
