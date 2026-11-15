package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.CustomFoodDraft
import com.fitai.app.model.FoodServing
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.Macros
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.nutrition.AddFoodToDiaryUseCase
import com.fitai.app.nutrition.DeleteCustomFoodUseCase
import com.fitai.app.nutrition.DeleteMealEntryUseCase
import com.fitai.app.nutrition.GetFoodDetailUseCase
import com.fitai.app.nutrition.SaveCustomFoodUseCase
import com.fitai.app.nutrition.SearchFoodsUseCase
import com.fitai.app.nutrition.UpdateLoggedFoodUseCase
import com.fitai.app.nutrition.withAmount
import com.fitai.app.viewmodel.nutrition.CustomFoodEffect
import com.fitai.app.viewmodel.nutrition.CustomFoodIntent
import com.fitai.app.viewmodel.nutrition.CustomFoodState
import com.fitai.app.viewmodel.nutrition.FoodDetailEffect
import com.fitai.app.viewmodel.nutrition.FoodDetailError
import com.fitai.app.viewmodel.nutrition.FoodDetailIntent
import com.fitai.app.viewmodel.nutrition.FoodDetailState
import com.fitai.app.viewmodel.nutrition.FoodSearchEffect
import com.fitai.app.viewmodel.nutrition.FoodSearchIntent
import com.fitai.app.viewmodel.nutrition.FoodSearchState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

class FoodSearchViewModel(
    private val searchFoodsUseCase: SearchFoodsUseCase,
) : BaseViewModel<FoodSearchState, FoodSearchIntent, FoodSearchEffect>(FoodSearchState()) {

    private var searchJob: Job? = null
    private var loadedKey: Pair<MealSlot, Int>? = null

    override fun handleIntent(intent: FoodSearchIntent) {
        when (intent) {
            is FoodSearchIntent.Load -> if (loadedKey != intent.slot to intent.session) {
                loadedKey = intent.slot to intent.session
                setState { FoodSearchState(slot = intent.slot) }
                search("", debounce = false)
            }
            is FoodSearchIntent.QueryChanged -> {
                setState { copy(query = intent.query, searched = intent.query.isNotBlank() && searched) }
                search(intent.query, debounce = true)
            }
            is FoodSearchIntent.Select -> setEffect(FoodSearchEffect.OpenFood(intent.food.id, currentState().slot))
            FoodSearchIntent.CreateCustom -> setEffect(FoodSearchEffect.OpenCustomFood(currentState().slot))
            FoodSearchIntent.QuickAdd -> setEffect(FoodSearchEffect.OpenQuickAdd(currentState().slot))
        }
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce && query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
            setState { copy(loading = true, failed = false) }
            when (val result = searchFoodsUseCase(query, currentState().slot.code)) {
                is UseCaseState.Success -> setState {
                    val data = result.data
                    if (data.query.isBlank()) {
                        copy(
                            loading = false,
                            results = emptyList(),
                            fromPlan = data.fromPlan,
                            recent = data.recent,
                            frequent = data.frequent,
                            custom = data.custom,
                            searched = false,
                        )
                    } else {
                        copy(loading = false, results = data.results, searched = true)
                    }
                }
                is UseCaseState.Error -> setState { copy(loading = false, failed = true) }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}

class FoodDetailViewModel(
    private val getFoodDetailUseCase: GetFoodDetailUseCase,
    private val addFoodToDiaryUseCase: AddFoodToDiaryUseCase,
    private val updateLoggedFoodUseCase: UpdateLoggedFoodUseCase,
    private val deleteMealEntryUseCase: DeleteMealEntryUseCase,
    private val deleteCustomFoodUseCase: DeleteCustomFoodUseCase,
) : BaseViewModel<FoodDetailState, FoodDetailIntent, FoodDetailEffect>(FoodDetailState()) {

    private var loadedKey: List<Any?>? = null

    override fun handleIntent(intent: FoodDetailIntent) {
        when (intent) {
            is FoodDetailIntent.Load -> load(intent)
            is FoodDetailIntent.UnitSelected -> {
                val s = currentState()
                // Keep the weight when switching measure: 2 slices (50 g) -> 50 g.
                val oldGrams = (s.servings.gramsPer(s.unit) ?: 1.0) * (s.amountText.toAmount() ?: 0.0)
                val newPer = s.servings.gramsPer(intent.unit) ?: 1.0
                val amount = if (oldGrams > 0) roundForUnit(oldGrams / newPer, intent.unit) else defaultAmount(intent.unit)
                update { copy(unit = intent.unit, amountText = amount.toAmountText(), error = null) }
            }
            is FoodDetailIntent.AmountChanged ->
                update { copy(amountText = intent.text.filter { it.isDigit() || it == '.' || it == ',' }.take(7), error = null) }
            is FoodDetailIntent.Step -> {
                val s = currentState()
                val step = stepFor(s.unit)
                val current = s.amountText.toAmount() ?: 0.0
                val next = (current + if (intent.up) step else -step).coerceAtLeast(step)
                update { copy(amountText = roundForUnit(next, unit).toAmountText(), error = null) }
            }
            is FoodDetailIntent.SlotSelected -> setState { copy(slot = intent.slot) }
            FoodDetailIntent.Save -> save()
            FoodDetailIntent.Delete -> delete()
            FoodDetailIntent.DeleteCustomFood -> deleteCustomFood()
        }
    }

    private fun load(intent: FoodDetailIntent.Load) {
        val key = listOf(intent.foodId, intent.mealId, intent.slot, intent.session)
        if (loadedKey == key) return
        loadedKey = key
        setState { FoodDetailState(loading = true, slot = intent.slot) }
        viewModelScope.launch {
            when (val result = getFoodDetailUseCase(intent.foodId, intent.mealId)) {
                is UseCaseState.Success -> {
                    val detail = result.data
                    val entry = detail.entry
                    val servings = detail.food?.pickerServings ?: entry.entryServings()
                    val unit = entry?.servingUnit?.takeIf { u -> servings.any { it.unit == u } }
                        ?: detail.lastServing?.unit?.takeIf { u -> servings.any { it.unit == u } }
                        ?: servings.first().unit
                    val last = detail.lastServing
                    val amount = entry?.servingAmount?.takeIf { entry.servingUnit == unit }
                        ?: last?.amount?.takeIf { last.unit == unit }
                        ?: defaultAmount(unit)
                    update {
                        copy(
                            loading = false,
                            food = detail.food,
                            entry = entry,
                            servings = servings,
                            unit = unit,
                            amountText = amount.toAmountText(),
                            slot = MealSlot.fromCode(entry?.slot) ?: intent.slot,
                        )
                    }
                }
                is UseCaseState.Error -> setState { copy(loading = false, error = FoodDetailError.LoadFailed) }
            }
        }
    }

    /** Applies [reducer] and recomputes the macro preview for the new amount. */
    private fun update(reducer: FoodDetailState.() -> FoodDetailState) {
        setState {
            val next = reducer()
            next.copy(preview = next.previewMacros())
        }
    }

    private fun save() {
        val s = currentState()
        if (s.saving || s.loading) return
        val amount = s.amountText.toAmount()?.takeIf { it > 0 }
        if (amount == null) {
            setState { copy(error = FoodDetailError.InvalidAmount) }
            return
        }
        viewModelScope.launch {
            setState { copy(saving = true, error = null) }
            val entry = s.entry
            val food = s.food
            val result = when {
                entry != null -> updateLoggedFoodUseCase(entry.id, s.unit, amount, s.slot)
                food != null -> addFoodToDiaryUseCase(food, s.unit, amount, s.slot)
                else -> UseCaseState.Error("food_not_found")
            }
            when (result) {
                is UseCaseState.Success -> {
                    setState { copy(saving = false) }
                    setEffect(FoodDetailEffect.Saved)
                }
                is UseCaseState.Error -> setState { copy(saving = false, error = FoodDetailError.SaveFailed) }
            }
        }
    }

    private fun delete() {
        val entry = currentState().entry ?: return
        viewModelScope.launch {
            setState { copy(saving = true) }
            when (deleteMealEntryUseCase(entry.id)) {
                is UseCaseState.Success -> setEffect(FoodDetailEffect.Deleted)
                is UseCaseState.Error -> setState { copy(saving = false, error = FoodDetailError.SaveFailed) }
            }
        }
    }

    private fun deleteCustomFood() {
        val food = currentState().food?.takeIf { it.isCustom } ?: return
        viewModelScope.launch {
            setState { copy(saving = true) }
            when (deleteCustomFoodUseCase(food.id)) {
                is UseCaseState.Success -> setEffect(FoodDetailEffect.Deleted)
                is UseCaseState.Error -> setState { copy(saving = false, error = FoodDetailError.SaveFailed) }
            }
        }
    }
}

/** Macros of the amount being picked: from the database food, or scaled from the logged entry. */
private fun FoodDetailState.previewMacros(): Macros? {
    val amount = amountText.toAmount() ?: return null
    food?.let { return it.macrosFor(unit, amount) }
    val logged = entry?.takeIf { it.isPortioned } ?: return null
    val scaled = logged.withAmount(null, unit, amount)
    return Macros(scaled.calories.toDouble(), scaled.proteinG, scaled.carbsG, scaled.fatG)
}

/** Measures for an entry whose food is not in the database (AI plan foods): its own unit, then grams. */
private fun MealLog?.entryServings(): List<FoodServing> {
    val grams = FoodServing(FoodUnits.GRAM, 1.0)
    val unit = this?.servingUnit ?: return listOf(grams)
    val amount = servingAmount?.takeIf { it > 0 } ?: return listOf(grams)
    val total = this.grams?.takeIf { it > 0 } ?: return listOf(grams)
    return if (unit == FoodUnits.GRAM) listOf(grams) else listOf(FoodServing(unit, total / amount), grams)
}

private fun List<FoodServing>.gramsPer(unit: String): Double? = firstOrNull { it.unit == unit }?.grams

private fun stepFor(unit: String): Double = if (unit == FoodUnits.GRAM || unit == FoodUnits.ML) 10.0 else 0.5

private fun defaultAmount(unit: String): Double = if (unit == FoodUnits.GRAM || unit == FoodUnits.ML) 100.0 else 1.0

private fun roundForUnit(value: Double, unit: String): Double {
    val step = if (unit == FoodUnits.GRAM || unit == FoodUnits.ML) 1.0 else 0.25
    return (floor(value / step + 0.5) * step).coerceAtLeast(step)
}

private fun String.toAmount(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

/** "2", "1.5", "0.25"; no trailing zeros. */
internal fun Double.toAmountText(): String {
    val hundredths = kotlin.math.round(this * 100).toLong()
    val whole = hundredths / 100
    val frac = hundredths % 100
    return when {
        frac == 0L -> "$whole"
        frac % 10 == 0L -> "$whole.${frac / 10}"
        else -> "$whole.${frac.toString().padStart(2, '0')}"
    }
}

class CustomFoodViewModel(
    private val saveCustomFoodUseCase: SaveCustomFoodUseCase,
) : BaseViewModel<CustomFoodState, CustomFoodIntent, CustomFoodEffect>(CustomFoodState()) {

    private var loadedSession: Int? = null

    override fun handleIntent(intent: CustomFoodIntent) {
        when (intent) {
            is CustomFoodIntent.Load -> if (loadedSession != intent.session) {
                loadedSession = intent.session
                setState { CustomFoodState(slot = intent.slot) }
            }
            is CustomFoodIntent.NameChanged -> setState { copy(name = intent.value.take(80), invalid = false) }
            is CustomFoodIntent.UnitSelected -> setState {
                copy(servingUnit = intent.unit, servingGrams = if (intent.unit == FoodUnits.GRAM) "100" else servingGrams, invalid = false)
            }
            is CustomFoodIntent.GramsChanged -> setState { copy(servingGrams = intent.value.numeric(), invalid = false) }
            is CustomFoodIntent.CaloriesChanged -> setState { copy(calories = intent.value.numeric(), invalid = false) }
            is CustomFoodIntent.ProteinChanged -> setState { copy(proteinG = intent.value.numeric(), invalid = false) }
            is CustomFoodIntent.CarbsChanged -> setState { copy(carbsG = intent.value.numeric(), invalid = false) }
            is CustomFoodIntent.FatChanged -> setState { copy(fatG = intent.value.numeric(), invalid = false) }
            CustomFoodIntent.Save -> save()
        }
    }

    private fun save() {
        val s = currentState()
        if (s.saving) return
        val grams = if (s.servingUnit == FoodUnits.GRAM) 100.0 else s.servingGrams.toMacroGrams()
        val draft = CustomFoodDraft(
            name = s.name.trim(),
            servingUnit = s.servingUnit,
            servingGrams = grams ?: 0.0,
            caloriesPerServing = s.calories.toMacroGrams() ?: -1.0,
            proteinPerServing = s.proteinG.toMacroGrams() ?: 0.0,
            carbsPerServing = s.carbsG.toMacroGrams() ?: 0.0,
            fatPerServing = s.fatG.toMacroGrams() ?: 0.0,
        )
        if (!draft.isValid) {
            setState { copy(invalid = true) }
            return
        }
        viewModelScope.launch {
            setState { copy(saving = true, invalid = false, failed = false) }
            when (val result = saveCustomFoodUseCase(draft)) {
                is UseCaseState.Success -> {
                    setState { copy(saving = false) }
                    setEffect(CustomFoodEffect.Saved(result.data.id, s.slot))
                }
                is UseCaseState.Error -> setState { copy(saving = false, failed = true) }
            }
        }
    }

    private fun String.numeric(): String = filter { it.isDigit() || it == '.' || it == ',' }.take(7)
}

/** The food's name in [locale], or the entry's stored name. */
fun FoodDetailState.displayName(locale: String): String = food?.name(locale) ?: entry?.mealName.orEmpty()
