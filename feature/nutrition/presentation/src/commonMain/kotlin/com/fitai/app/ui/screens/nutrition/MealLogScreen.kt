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
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.MealSlot
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.MealLogViewModel
import com.fitai.app.viewmodel.nutrition.MealFormFields
import com.fitai.app.viewmodel.nutrition.MealLogIntent
import com.fitai.app.viewmodel.nutrition.NutritionEffect
import org.koin.compose.viewmodel.koinViewModel

/** Quick add by totals, or editing a photo / quick-add entry when [mealId] is set. */
@Composable
fun MealLogScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    slot: MealSlot? = null,
    mealId: String? = null,
    session: Int = 0,
    viewModel: MealLogViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(mealId, slot, session) { viewModel.onIntent(MealLogIntent.Load(mealId, slot, session)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is NutritionEffect.MealSaved) onSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .padding(horizontal = FitAISpacing.lg),
    ) {
        NutritionTopBar(
            title = if (state.editingId != null) strings.mealEntryEditTitle else strings.foodQuickAdd,
            onBack = onBack,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            SectionLabel(strings.foodMeal)
            SlotChips(selected = state.slot, onSelect = { viewModel.onIntent(MealLogIntent.SlotSelected(it)) }, enabled = !state.saving)
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
                Text(strings.foodQuickAddInvalid, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Error)
            }
            if (state.editingId != null) {
                FitAIButton(
                    text = strings.foodDeleteEntry,
                    onClick = { viewModel.onIntent(MealLogIntent.Delete) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = FitAIButtonVariant.Ghost,
                    enabled = !state.saving,
                    leadingIcon = FitAIIcons.Delete,
                )
            }
        }
        FitAIButton(
            text = strings.actionSave,
            onClick = { viewModel.onIntent(MealLogIntent.Save) },
            modifier = Modifier.fillMaxWidth().padding(vertical = FitAISpacing.md),
            enabled = !state.saving,
        )
    }
}
