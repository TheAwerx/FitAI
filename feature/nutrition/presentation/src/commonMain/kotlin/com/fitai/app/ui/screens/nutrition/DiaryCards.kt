package com.fitai.app.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.Macros
import com.fitai.app.model.MealLog
import com.fitai.app.model.PlanMealOption
import com.fitai.app.nutrition.DayMacroTargets
import com.fitai.app.nutrition.DiarySlot
import com.fitai.app.nutrition.TodayNutritionOverview
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAIProgressRing
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.toAmountText
import kotlin.math.roundToInt

/** Today: calorie ring (eaten of the day's target) next to eaten / goal / left, then macro bars. */
@Composable
internal fun DailySummaryCard(
    overview: TodayNutritionOverview,
    modifier: Modifier = Modifier,
) {
    val targets = overview.targets ?: return
    val consumed = overview.consumed
    val strings = LocalStrings.current
    val left = targets.calories - consumed.calories
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(strings.nutritionRemainingTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                Text(
                    text = if (targets.isTrainingDay) strings.nutritionTrainingDayTarget else strings.nutritionRestDayTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.Primary,
                    modifier = Modifier
                        .background(FitAIColors.Primary.copy(alpha = 0.12f), FitAIShapes.chip)
                        .padding(horizontal = FitAISpacing.sm, vertical = FitAISpacing.xs),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FitAIProgressRing(
                        progress = if (targets.calories > 0) consumed.calories.toFloat() / targets.calories else 0f,
                        size = 112.dp,
                        strokeWidth = 10.dp,
                        progressColor = if (left < 0) FitAIColors.Warning else FitAIColors.Primary,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${kotlin.math.abs(left)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = FitAIColors.OnSurface,
                        )
                        Text(
                            text = if (left < 0) strings.nutritionRingOver else strings.nutritionRingLeft,
                            style = MaterialTheme.typography.labelSmall,
                            color = FitAIColors.OnSurfaceMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                    SummaryStat(strings.nutritionEaten, strings.foodKcal(consumed.calories))
                    SummaryStat(strings.nutritionGoal, strings.foodKcal(targets.calories))
                    SummaryStat(
                        label = strings.nutritionRemaining,
                        value = if (left >= 0) strings.foodKcal(left) else strings.nutritionKcalOver(-left),
                        warn = left < 0,
                    )
                }
            }
            MacroBars(consumed = Macros(consumed.calories.toDouble(), consumed.proteinG, consumed.carbsG, consumed.fatG), targets = targets)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, warn: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (warn) FitAIColors.Warning else FitAIColors.OnSurface)
    }
}

@Composable
private fun MacroBars(consumed: Macros, targets: DayMacroTargets) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        MacroProgressRow(
            label = strings.nutritionProtein,
            value = strings.nutritionGramsProgress(consumed.proteinG.roundToInt(), targets.proteinG),
            consumed = consumed.proteinG,
            target = targets.proteinG.toDouble(),
        )
        MacroProgressRow(
            label = strings.nutritionCarbs,
            value = strings.nutritionGramsProgress(consumed.carbsG.roundToInt(), targets.carbsG),
            consumed = consumed.carbsG,
            target = targets.carbsG.toDouble(),
        )
        MacroProgressRow(
            label = strings.nutritionFat,
            value = strings.nutritionGramsProgress(consumed.fatG.roundToInt(), targets.fatG),
            consumed = consumed.fatG,
            target = targets.fatG.toDouble(),
        )
    }
}

/** The plan's selected meals added up, against the plan's daily targets. */
@Composable
internal fun PlanTotalsCard(
    totals: Macros,
    dailyCalories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(strings.nutritionPlanTotalsTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                Text(
                    strings.nutritionPlanTotalsBody(totals.calories.roundToInt(), dailyCalories),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FitAIColors.Primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                PlanMacro(strings.nutritionProtein, strings.nutritionPlanMacrosVsTarget(totals.proteinG.roundToInt(), proteinG), Modifier.weight(1f))
                PlanMacro(strings.nutritionCarbs, strings.nutritionPlanMacrosVsTarget(totals.carbsG.roundToInt(), carbsG), Modifier.weight(1f))
                PlanMacro(strings.nutritionFat, strings.nutritionPlanMacrosVsTarget(totals.fatG.roundToInt(), fatG), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlanMacro(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
    }
}

/**
 * One meal of the day, FatSecret style: header with time, logged and planned calories and "+", the logged
 * entries, then the plan for the meal (foods with portions and macros, prep note, "ate as planned") and its
 * alternatives, each swappable with one tap.
 */
@Composable
internal fun MealSlotCard(
    slot: DiarySlot,
    planExpanded: Boolean,
    busy: Boolean,
    onAdd: () -> Unit,
    onOpenEntry: (MealLog) -> Unit,
    onDeleteEntry: (MealLog) -> Unit,
    onLogPlanned: () -> Unit,
    onSwap: (Int) -> Unit,
    onTogglePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val plan = slot.plan
    val option = plan?.current
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.mealSlotLabel(slot.slot), style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                    val meta = buildList {
                        add(slot.time)
                        option?.let { add(strings.nutritionPlannedKcal(it.macros.calories.roundToInt())) }
                    }.joinToString(" · ")
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                }
                Text(
                    text = strings.foodKcal(slot.consumed.calories.roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (slot.entries.isEmpty()) FitAIColors.OnSurfaceMuted else FitAIColors.OnSurface,
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).background(FitAIColors.Primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(FitAIIcons.Add, contentDescription = strings.nutritionAddFood, tint = FitAIColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (slot.entries.isEmpty() && plan == null) {
                Text(strings.nutritionSlotEmpty, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
            }
            slot.entries.forEach { entry ->
                EntryRow(entry = entry, onClick = { onOpenEntry(entry) }, onDelete = { onDeleteEntry(entry) })
            }
            if (plan != null && option != null) {
                PlanBlock(
                    option = option,
                    alternatives = plan.alternatives,
                    expanded = planExpanded,
                    busy = busy,
                    onToggle = onTogglePlan,
                    onLogPlanned = onLogPlanned,
                    onSwap = onSwap,
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: MealLog, onClick: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    val detail = buildList {
        val unit = entry.servingUnit
        val amount = entry.servingAmount
        val grams = entry.grams
        if (unit != null && amount != null && grams != null) add(strings.foodPortion(amount.toAmountText(), unit, grams.roundToInt()))
        add(strings.macroLine(entry.proteinG, entry.carbsG, entry.fatG))
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FitAIShapes.button)
            .background(FitAIColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = FitAISpacing.md, top = FitAISpacing.xs, bottom = FitAISpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.hasPhoto) {
            Icon(FitAIIcons.PhotoCamera, contentDescription = null, tint = FitAIColors.OnSurfaceMuted, modifier = Modifier.size(16.dp).padding(end = 0.dp))
            Box(Modifier.size(FitAISpacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.mealName, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(strings.foodKcal(entry.calories), style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(FitAIIcons.Delete, contentDescription = strings.foodDeleteEntry, tint = FitAIColors.OnSurfaceMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PlanBlock(
    option: PlanMealOption,
    alternatives: List<IndexedValue<PlanMealOption>>,
    expanded: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onLogPlanned: () -> Unit,
    onSwap: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FitAIColors.SurfaceVariant, FitAIShapes.button)
            .padding(FitAISpacing.md),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.nutritionTodayPlan, style = MaterialTheme.typography.labelSmall, color = FitAIColors.Primary)
                Text(option.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = FitAIColors.OnSurface)
            }
            Icon(
                imageVector = FitAIIcons.ExpandMore,
                contentDescription = if (expanded) strings.nutritionHidePlan else strings.nutritionShowPlan,
                tint = FitAIColors.OnSurfaceMuted,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
        if (!expanded) return@Column
        option.foods.forEach { food ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Box(Modifier.padding(top = 7.dp).size(6.dp).background(FitAIColors.Primary, CircleShape))
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
                    Text(
                        "${strings.portionText(food)} · ${strings.macroLine(food.proteinG, food.carbsG, food.fatG)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitAIColors.OnSurfaceMuted,
                    )
                }
                Text(strings.foodKcal(food.calories), style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
            }
        }
        val macros = option.macros
        Text(
            text = strings.nutritionNextMealMacros(macros.calories.roundToInt(), macros.proteinG.roundToInt(), macros.carbsG.roundToInt(), macros.fatG.roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            color = FitAIColors.Primary,
        )
        if (option.note.isNotBlank()) {
            Text(
                text = "${strings.nutritionPrepNote}: ${option.note}",
                style = MaterialTheme.typography.bodyMedium,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
        FitAIButton(
            text = strings.nutritionAteAsPlanned,
            onClick = onLogPlanned,
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Secondary,
            enabled = !busy,
            leadingIcon = FitAIIcons.Check,
        )
        if (alternatives.isNotEmpty()) {
            Text(strings.nutritionAlternatives, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            alternatives.forEachIndexed { position, (index, alternative) ->
                AlternativeRow(
                    label = strings.nutritionAlternativeLabel(position + 1),
                    option = alternative,
                    enabled = !busy,
                    onSwap = { onSwap(index) },
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally),
                color = FitAIColors.Primary,
                trackColor = FitAIColors.SurfaceVariant,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun AlternativeRow(label: String, option: PlanMealOption, enabled: Boolean, onSwap: () -> Unit) {
    val strings = LocalStrings.current
    val macros = option.macros
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FitAIShapes.button)
            .background(FitAIColors.SurfaceVariant)
            .padding(start = FitAISpacing.md, top = FitAISpacing.sm, end = FitAISpacing.xs),
    ) {
        Text("$label · ${option.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = FitAIColors.OnSurface)
        Text(
            option.foods.joinToString(", ") { "${it.name} (${strings.portionText(it)})" },
            style = MaterialTheme.typography.labelSmall,
            color = FitAIColors.OnSurfaceMuted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = strings.nutritionNextMealMacros(macros.calories.roundToInt(), macros.proteinG.roundToInt(), macros.carbsG.roundToInt(), macros.fatG.roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            color = FitAIColors.Primary,
            modifier = Modifier.padding(top = FitAISpacing.xs),
        )
        FitAIButton(
            text = strings.nutritionSwapIn,
            onClick = onSwap,
            modifier = Modifier.align(Alignment.End),
            variant = FitAIButtonVariant.Ghost,
            enabled = enabled,
            leadingIcon = FitAIIcons.SwapHoriz,
        )
    }
}

/** Thin labelled progress bar, shared by the daily summary. */
@Composable
internal fun MacroProgressRow(
    label: String,
    value: String,
    consumed: Double,
    target: Double,
) {
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    val over = target > 0 && consumed > target * 1.05
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurface)
            Text(value, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(FitAIColors.SurfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(if (over) FitAIColors.Warning else FitAIColors.Primary),
            )
        }
    }
}
