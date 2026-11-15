package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.model.FoodItem
import com.fitai.app.model.MealSlot
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.util.appLocale
import com.fitai.app.viewmodel.FoodSearchViewModel
import com.fitai.app.viewmodel.nutrition.FoodSearchEffect
import com.fitai.app.viewmodel.nutrition.FoodSearchIntent
import com.fitai.app.viewmodel.nutrition.FoodSearchState
import org.koin.compose.viewmodel.koinViewModel

/** Live search over the food database for one meal; recent, frequent and plan foods before typing. */
@Composable
fun FoodSearchScreen(
    slot: MealSlot,
    onBack: () -> Unit,
    onOpenFood: (foodId: String, slot: MealSlot) -> Unit,
    onCreateCustom: (MealSlot) -> Unit,
    onQuickAdd: (MealSlot) -> Unit,
    modifier: Modifier = Modifier,
    session: Int = 0,
    viewModel: FoodSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(slot, session) { viewModel.onIntent(FoodSearchIntent.Load(slot, session)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FoodSearchEffect.OpenFood -> onOpenFood(effect.foodId, effect.slot)
                is FoodSearchEffect.OpenCustomFood -> onCreateCustom(effect.slot)
                is FoodSearchEffect.OpenQuickAdd -> onQuickAdd(effect.slot)
            }
        }
    }
    val locale = appLocale()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .padding(horizontal = FitAISpacing.lg),
    ) {
        NutritionTopBar(title = strings.mealSlotLabel(slot), onBack = onBack)
        FitAITextField(
            value = state.query,
            onValueChange = { viewModel.onIntent(FoodSearchIntent.QueryChanged(it)) },
            label = strings.nutritionAddFood,
            placeholder = strings.foodSearchPlaceholder,
            leadingIcon = FitAIIcons.Search,
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.onIntent(FoodSearchIntent.QueryChanged("")) }) {
                        Icon(FitAIIcons.Close, contentDescription = strings.actionClose, tint = FitAIColors.OnSurfaceMuted)
                    }
                }
            } else {
                null
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        ) {
            item { Row(Modifier.padding(top = FitAISpacing.sm)) {} }
            if (state.query.isBlank()) {
                browseSections(state, strings, locale) { viewModel.onIntent(FoodSearchIntent.Select(it)) }
            } else {
                items(state.results, key = { "r_" + it.id }) { food ->
                    FoodRow(
                        title = food.name(locale),
                        subtitle = strings.foodSubtitle(food),
                        kcal = food.defaultServingKcal(),
                        onClick = { viewModel.onIntent(FoodSearchIntent.Select(food)) },
                    )
                }
                if (state.searched && state.results.isEmpty() && !state.loading) {
                    item { Hint(strings.foodSearchNoResults) }
                }
            }
            if (state.failed) item { Hint(strings.errorUnknown) }
            item {
                Column(
                    modifier = Modifier.padding(vertical = FitAISpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                ) {
                    FitAIButton(
                        text = strings.foodCreateCustom,
                        onClick = { viewModel.onIntent(FoodSearchIntent.CreateCustom) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = FitAIButtonVariant.Secondary,
                        leadingIcon = FitAIIcons.Add,
                    )
                    FitAIButton(
                        text = strings.foodQuickAdd,
                        onClick = { viewModel.onIntent(FoodSearchIntent.QuickAdd) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = FitAIButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.browseSections(
    state: FoodSearchState,
    strings: Strings,
    locale: String,
    onSelect: (FoodItem) -> Unit,
) {
    val sections = listOf(
        strings.foodSearchFromPlan to state.fromPlan,
        strings.foodSearchRecent to state.recent,
        strings.foodSearchFrequent to state.frequent,
        strings.foodSearchMine to state.custom,
    ).filter { it.second.isNotEmpty() }
    if (sections.isEmpty() && !state.loading) {
        item { Hint(strings.foodSearchEmptyHint) }
    }
    sections.forEach { (title, foods) ->
        item(key = "h_$title") { SectionLabel(title, Modifier.padding(top = FitAISpacing.sm)) }
        items(foods, key = { "${title}_${it.id}" }) { food ->
            FoodRow(
                title = food.name(locale),
                subtitle = strings.foodSubtitle(food),
                kcal = food.defaultServingKcal(),
                onClick = { onSelect(food) },
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = FitAISpacing.sm),
        style = MaterialTheme.typography.bodyMedium,
        color = FitAIColors.OnSurfaceMuted,
    )
}
