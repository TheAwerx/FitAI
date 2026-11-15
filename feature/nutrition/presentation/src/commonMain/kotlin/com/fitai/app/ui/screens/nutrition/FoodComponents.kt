package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodServing
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.MealSlot
import com.fitai.app.model.PlanFood
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.toAmountText
import kotlin.math.roundToInt

/** 64 dp top bar with a back button, same as the other tab sub-screens. */
@Composable
internal fun NutritionTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = FitAIIcons.ArrowBack,
                contentDescription = LocalStrings.current.actionBack,
                tint = FitAIColors.Primary,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FitAIColors.OnBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
internal fun NutritionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant, FitAIShapes.chip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FitAIColors.OnPrimary else FitAIColors.OnSurface,
            maxLines = 1,
        )
    }
}

/** A horizontally scrolling chip row (meal slots, serving measures). */
@Composable
internal fun <T> ChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        items.forEach { item ->
            NutritionChip(label = label(item), selected = item == selected, onClick = { onSelect(item) }, enabled = enabled)
        }
    }
}

@Composable
internal fun SlotChips(
    selected: MealSlot,
    onSelect: (MealSlot) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalStrings.current
    ChipRow(MealSlot.entries, selected, { strings.mealSlotLabel(it) }, onSelect, modifier, enabled)
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = FitAIColors.OnSurface,
    )
}

/** One food in a list: name, a muted detail line and the calories on the right. */
@Composable
internal fun FoodRow(
    title: String,
    subtitle: String,
    kcal: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FitAIShapes.button)
            .background(FitAIColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm + FitAISpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = LocalStrings.current.foodKcal(kcal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = FitAIColors.OnSurface,
        )
    }
}

/** "1 dilim (25 g)" or "100 g". */
internal fun Strings.servingLabel(serving: FoodServing): String =
    if (serving.unit == FoodUnits.GRAM || serving.unit == FoodUnits.ML) {
        foodUnitLabel(serving.unit, 1.0)
    } else {
        "${foodUnitLabel(serving.unit, 1.0)} (${serving.grams.toAmountText()} g)"
    }

/** "Peynir · 1 dilim (30 g)" for search rows; the kcal goes on the right. */
internal fun Strings.foodSubtitle(food: FoodItem): String {
    val serving = food.defaultServing
    val portion = if (serving.unit == FoodUnits.GRAM) foodPortion("100", FoodUnits.GRAM, 100) else "1 ${servingLabel(serving)}"
    return "${foodCategoryLabel(food.category)} · $portion"
}

internal fun FoodItem.defaultServingKcal(): Int {
    val serving = defaultServing
    val grams = if (serving.unit == FoodUnits.GRAM) 100.0 else serving.grams
    return macrosFor(grams).calories.roundToInt()
}

internal fun Strings.portionText(food: PlanFood): String =
    foodPortion(food.amount.toAmountText(), food.unit, food.grams.roundToInt())

internal fun Strings.macroLine(proteinG: Double, carbsG: Double, fatG: Double): String =
    nutritionMealMacros(proteinG.roundToInt(), carbsG.roundToInt(), fatG.roundToInt())
