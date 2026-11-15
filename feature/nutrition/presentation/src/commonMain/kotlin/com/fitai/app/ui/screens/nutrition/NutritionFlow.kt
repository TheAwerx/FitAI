package com.fitai.app.ui.screens.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.model.MealSlot
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Screens of the Nutrition tab, pushed on top of the diary. [session] is unique per push: the screen's
 * ViewModel starts fresh for a new push and keeps its state when the user comes back to it.
 */
private sealed interface NutritionDestination {
    val session: Int

    data class Search(val slot: MealSlot, override val session: Int) : NutritionDestination
    data class FoodDetail(val foodId: String?, val slot: MealSlot, val mealId: String?, override val session: Int) : NutritionDestination
    data class CustomFood(val slot: MealSlot, override val session: Int) : NutritionDestination
    data class QuickAdd(val slot: MealSlot?, val mealId: String?, override val session: Int) : NutritionDestination
}

/**
 * The Nutrition tab with its own back stack: diary → food search → serving picker (or custom food,
 * quick add). [openQuickLog] (from Home's "log a meal") opens the food search for the current meal.
 */
@Composable
fun NutritionFlow(
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
    openQuickLog: Boolean = false,
    onQuickLogOpened: () -> Unit = {},
    onNestedChange: (Boolean) -> Unit = {},
) {
    var stack by remember { mutableStateOf(emptyList<NutritionDestination>()) }
    fun nextSession(): Int = NutritionSessionIds.next()
    val nested = stack.isNotEmpty()
    LaunchedEffect(nested) { onNestedChange(nested) }
    DisposableEffect(Unit) { onDispose { onNestedChange(false) } }
    fun push(destination: NutritionDestination) {
        stack = stack + destination
    }
    fun pop() {
        stack = stack.dropLast(1)
    }
    fun backToDiary() {
        stack = emptyList()
    }

    LaunchedEffect(openQuickLog) {
        if (openQuickLog) {
            stack = listOf(NutritionDestination.Search(MealSlot.forTimeNow(), nextSession()))
            onQuickLogOpened()
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = stack.isNotEmpty(),
        onBackCompleted = { pop() },
    )

    when (val top = stack.lastOrNull()) {
        null -> NutritionScreen(
            onNavigateMealLog = { slot, mealId -> push(NutritionDestination.QuickAdd(slot, mealId, nextSession())) },
            onNavigateFoodSearch = { slot -> push(NutritionDestination.Search(slot, nextSession())) },
            onNavigateFoodDetail = { foodId, slot, mealId ->
                push(NutritionDestination.FoodDetail(foodId, slot, mealId, nextSession()))
            },
            onNavigatePaywall = onNavigatePaywall,
            modifier = modifier,
        )
        is NutritionDestination.Search -> FoodSearchScreen(
            slot = top.slot,
            onBack = ::pop,
            onOpenFood = { foodId, slot -> push(NutritionDestination.FoodDetail(foodId, slot, null, nextSession())) },
            onCreateCustom = { slot -> push(NutritionDestination.CustomFood(slot, nextSession())) },
            onQuickAdd = { slot -> push(NutritionDestination.QuickAdd(slot, null, nextSession())) },
            modifier = modifier,
            session = top.session,
        )
        is NutritionDestination.FoodDetail -> FoodDetailScreen(
            foodId = top.foodId,
            mealId = top.mealId,
            slot = top.slot,
            onBack = ::pop,
            onDone = ::backToDiary,
            modifier = modifier,
            session = top.session,
        )
        is NutritionDestination.CustomFood -> CustomFoodScreen(
            slot = top.slot,
            onBack = ::pop,
            onSaved = { foodId, slot ->
                // Replace the form with the serving picker so back returns to the search.
                stack = stack.dropLast(1) + NutritionDestination.FoodDetail(foodId, slot, null, nextSession())
            },
            modifier = modifier,
            session = top.session,
        )
        is NutritionDestination.QuickAdd -> MealLogScreen(
            onBack = ::pop,
            onSaved = ::backToDiary,
            slot = top.slot,
            mealId = top.mealId,
            session = top.session,
            modifier = modifier,
        )
    }
}

/**
 * Push ids for the whole process. The tab's screen ViewModels outlive [NutritionFlow] (it leaves composition
 * on a tab switch); a counter kept inside it restarted at 1 and the next search reopened with the old query.
 */
private object NutritionSessionIds {
    private var last = 0

    fun next(): Int = ++last
}

@OptIn(ExperimentalTime::class)
private fun MealSlot.Companion.forTimeNow(): MealSlot {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return MealSlot.forTime(now.hour, now.minute, MealSlot.defaultDiary)
}
