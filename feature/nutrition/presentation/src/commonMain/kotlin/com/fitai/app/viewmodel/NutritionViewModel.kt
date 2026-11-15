package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.GatedFeature
import com.fitai.app.domain.GenerateDietPlanUseCase
import com.fitai.app.domain.LogMealUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.MealAnalysisResult
import com.fitai.app.model.MealSlot
import com.fitai.app.nutrition.AnalyzeMealPhotoWithQuotaUseCase
import com.fitai.app.nutrition.DeleteMealEntryUseCase
import com.fitai.app.nutrition.EditMealEntryUseCase
import com.fitai.app.nutrition.GetMealEntryUseCase
import com.fitai.app.nutrition.GetTodayNutritionOverviewUseCase
import com.fitai.app.nutrition.LogAnalyzedMealUseCase
import com.fitai.app.nutrition.LogPlannedMealUseCase
import com.fitai.app.nutrition.MealPhotoAnalysisOutcome
import com.fitai.app.nutrition.NextMealOutcome
import com.fitai.app.nutrition.ObserveNutritionOverviewUseCase
import com.fitai.app.nutrition.SelectPlanMealOptionUseCase
import com.fitai.app.nutrition.SuggestNextMealUseCase
import com.fitai.app.viewmodel.nutrition.MealFormFields
import com.fitai.app.viewmodel.nutrition.MealLogIntent
import com.fitai.app.viewmodel.nutrition.MealLogState
import com.fitai.app.viewmodel.nutrition.NextMealState
import com.fitai.app.viewmodel.nutrition.NutritionEffect
import com.fitai.app.viewmodel.nutrition.NutritionIntent
import com.fitai.app.viewmodel.nutrition.NutritionState
import com.fitai.app.viewmodel.nutrition.PlatePhotoState
import com.fitai.app.viewmodel.nutrition.ReviewError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class NutritionViewModel(
    private val observeNutritionOverviewUseCase: ObserveNutritionOverviewUseCase,
    private val getTodayNutritionOverviewUseCase: GetTodayNutritionOverviewUseCase,
    private val generateDietPlanUseCase: GenerateDietPlanUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val analyzeMealPhotoWithQuotaUseCase: AnalyzeMealPhotoWithQuotaUseCase,
    private val logAnalyzedMealUseCase: LogAnalyzedMealUseCase,
    private val suggestNextMealUseCase: SuggestNextMealUseCase,
    private val logPlannedMealUseCase: LogPlannedMealUseCase,
    private val selectPlanMealOptionUseCase: SelectPlanMealOptionUseCase,
    private val deleteMealEntryUseCase: DeleteMealEntryUseCase,
) : BaseViewModel<NutritionState, NutritionIntent, NutritionEffect>(NutritionState()) {

    private var observeJob: Job? = null
    private var nextMealJob: Job? = null

    init {
        refresh()
    }

    override fun handleIntent(intent: NutritionIntent) {
        when (intent) {
            NutritionIntent.Refresh -> refresh()
            NutritionIntent.RefreshAccess -> {
                refreshPhotoAccess()
                if (currentState().nextMeal == NextMealState.Locked) suggestNextMeal()
            }
            NutritionIntent.RegeneratePlan -> regenerate()
            NutritionIntent.OpenManualLog -> setEffect(NutritionEffect.NavigateToMealLog(slot = currentSlot()))
            NutritionIntent.OpenPaywall -> setEffect(NutritionEffect.NavigateToPaywall)
            is NutritionIntent.PhotoPicked -> analyzePhoto(intent.bytes, intent.mimeType)
            NutritionIntent.PhotoPickCancelled -> Unit
            is NutritionIntent.ReviewChanged -> updateReview(intent.fields)
            NutritionIntent.SaveAnalyzedMeal -> saveAnalyzedMeal()
            NutritionIntent.DiscardAnalysis -> setState { copy(plate = PlatePhotoState.Idle) }
            NutritionIntent.RetryNextMeal -> suggestNextMeal()
            is NutritionIntent.AddFood -> setEffect(NutritionEffect.NavigateToFoodSearch(intent.slot))
            is NutritionIntent.OpenEntry -> openEntry(intent)
            is NutritionIntent.DeleteEntry -> viewModelScope.launch { deleteMealEntryUseCase(intent.mealId) }
            is NutritionIntent.LogPlanned -> logPlanned(intent.slot)
            is NutritionIntent.SwapOption -> swap(intent.slot, intent.optionIndex)
            is NutritionIntent.TogglePlan -> setState {
                val code = intent.slot.code
                copy(toggledPlans = if (code in toggledPlans) toggledPlans - code else toggledPlans + code)
            }
        }
    }

    private fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeNutritionOverviewUseCase().collectLatest { overview ->
                setState {
                    copy(
                        activePlan = overview.activePlan,
                        planHistory = overview.planHistory,
                        todayMeals = overview.todayMeals,
                        diary = overview.diary,
                        planTotals = overview.planTotals,
                        loading = false,
                    )
                }
                val today = getTodayNutritionOverviewUseCase()
                if (today is UseCaseState.Success) setState { copy(today = today.data) }
            }
        }
        refreshPhotoAccess()
    }

    private fun openEntry(intent: NutritionIntent.OpenEntry) {
        val meal = intent.meal
        val slot = MealSlot.fromCode(meal.slot) ?: currentState().diary.firstOrNull { d -> d.entries.any { it.id == meal.id } }?.slot
            ?: currentSlot()
        if (meal.isPortioned) {
            setEffect(NutritionEffect.NavigateToFoodDetail(meal.foodId, slot, meal.id))
        } else {
            setEffect(NutritionEffect.NavigateToMealLog(slot = slot, mealId = meal.id))
        }
    }

    private fun logPlanned(slot: MealSlot) {
        if (currentState().busySlot != null) return
        viewModelScope.launch {
            setState { copy(busySlot = slot.code) }
            val result = logPlannedMealUseCase(slot.code)
            setState { copy(busySlot = null) }
            if (result is UseCaseState.Success) setEffect(NutritionEffect.PlannedMealLogged)
        }
    }

    private fun swap(slot: MealSlot, optionIndex: Int) {
        if (currentState().busySlot != null) return
        viewModelScope.launch {
            setState { copy(busySlot = slot.code) }
            selectPlanMealOptionUseCase(slot.code, optionIndex)
            setState { copy(busySlot = null) }
        }
    }

    /** The diary meal closest to the current time. */
    @OptIn(ExperimentalTime::class)
    private fun currentSlot(): MealSlot {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val slots = currentState().diary.map { it.slot }.ifEmpty { MealSlot.defaultDiary }
        return MealSlot.forTime(now.hour, now.minute, slots)
    }

    private fun refreshPhotoAccess() {
        viewModelScope.launch {
            val access = checkFeatureAccessUseCase(GatedFeature.MealPhotoAnalysis)
            if (access is UseCaseState.Success) setState { copy(photoAccess = access.data) }
        }
    }

    private fun analyzePhoto(bytes: ByteArray, mimeType: String) {
        if (currentState().plate == PlatePhotoState.Analyzing) return
        nextMealJob?.cancel()
        setState { copy(plate = PlatePhotoState.Analyzing, nextMeal = NextMealState.Hidden) }
        viewModelScope.launch {
            when (val result = analyzeMealPhotoWithQuotaUseCase(bytes, mimeType)) {
                is UseCaseState.Success -> when (val outcome = result.data) {
                    is MealPhotoAnalysisOutcome.Analyzed -> {
                        setState { copy(plate = PlatePhotoState.Review(outcome.result.toFormFields())) }
                        refreshPhotoAccess()
                    }
                    is MealPhotoAnalysisOutcome.Locked ->
                        setState { copy(plate = PlatePhotoState.Idle, photoAccess = outcome.access) }
                }
                is UseCaseState.Error -> setState { copy(plate = PlatePhotoState.Failed) }
            }
        }
    }

    private fun updateReview(fields: MealFormFields) {
        val review = currentState().plate as? PlatePhotoState.Review ?: return
        setState {
            copy(
                plate = review.copy(
                    fields = fields.copy(calories = fields.calories.filter(Char::isDigit)),
                    error = null,
                ),
            )
        }
    }

    private fun saveAnalyzedMeal() {
        val review = currentState().plate as? PlatePhotoState.Review ?: return
        if (review.saving) return
        val fields = review.fields
        val calories = fields.calories.toIntOrNull()
        val protein = fields.proteinG.toMacroGrams()
        val carbs = fields.carbsG.toMacroGrams()
        val fat = fields.fatG.toMacroGrams()
        if (fields.mealName.isBlank() || calories == null || protein == null || carbs == null || fat == null) {
            setState { copy(plate = review.copy(error = ReviewError.Invalid)) }
            return
        }
        viewModelScope.launch {
            setState { copy(plate = review.copy(saving = true, error = null)) }
            when (logAnalyzedMealUseCase(fields.mealName.trim(), calories, protein, carbs, fat, currentSlot().code)) {
                is UseCaseState.Success -> {
                    setState { copy(plate = PlatePhotoState.Idle) }
                    setEffect(NutritionEffect.MealSaved)
                    suggestNextMeal()
                }
                is UseCaseState.Error ->
                    setState { copy(plate = review.copy(saving = false, error = ReviewError.SaveFailed)) }
            }
        }
    }

    private fun suggestNextMeal() {
        if (currentState().activePlan == null) {
            setState { copy(nextMeal = NextMealState.Hidden) }
            return
        }
        nextMealJob?.cancel()
        nextMealJob = viewModelScope.launch {
            setState { copy(nextMeal = NextMealState.Loading) }
            val next = when (val result = suggestNextMealUseCase()) {
                is UseCaseState.Success -> when (val outcome = result.data) {
                    is NextMealOutcome.Suggested -> NextMealState.Ready(outcome.suggestion)
                    is NextMealOutcome.Locked -> NextMealState.Locked
                }
                is UseCaseState.Error -> NextMealState.Failed
            }
            setState { copy(nextMeal = next) }
        }
    }

    private fun regenerate() {
        viewModelScope.launch {
            setState { copy(generating = true, errorMessage = null) }
            when (val result = generateDietPlanUseCase()) {
                is UseCaseState.Success -> setState { copy(generating = false) }
                is UseCaseState.Error -> {
                    if (result.message == "trial_expired") {
                        setEffect(NutritionEffect.NavigateToPaywall)
                    }
                    setState { copy(generating = false, errorMessage = result.message) }
                }
            }
        }
    }
}

private fun MealAnalysisResult.toFormFields() = MealFormFields(
    mealName = mealName,
    calories = calories.coerceAtLeast(0).toString(),
    proteinG = proteinG.toFieldText(),
    carbsG = carbsG.toFieldText(),
    fatG = fatG.toFieldText(),
)

/** One decimal at most, without a trailing ".0". */
internal fun Double.toFieldText(): String {
    val tenths = (coerceAtLeast(0.0) * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${abs(tenths % 10)}"
}

/** Accepts both "12.5" and the Turkish "12,5". */
internal fun String.toMacroGrams(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

/** Like [toMacroGrams], but an empty field is 0 g (quick add asks only for calories). */
internal fun String.toOptionalMacroGrams(): Double? = if (isBlank()) 0.0 else toMacroGrams()

/** Quick add of a meal by its totals, or editing a photo / quick-add entry ([MealLogState.editingId]). */
class MealLogViewModel(
    private val logMealUseCase: LogMealUseCase,
    private val getMealEntryUseCase: GetMealEntryUseCase,
    private val editMealEntryUseCase: EditMealEntryUseCase,
    private val deleteMealEntryUseCase: DeleteMealEntryUseCase,
) : BaseViewModel<MealLogState, MealLogIntent, NutritionEffect>(MealLogState()) {

    private var loadedKey: Triple<String?, MealSlot?, Int>? = null

    override fun handleIntent(intent: MealLogIntent) {
        when (intent) {
            is MealLogIntent.Load -> load(intent.mealId, intent.slot, intent.session)
            is MealLogIntent.MealNameChanged -> setState { copy(mealName = intent.value, errorMessage = null) }
            is MealLogIntent.CaloriesChanged ->
                setState { copy(calories = intent.value.filter(Char::isDigit), errorMessage = null) }
            is MealLogIntent.ProteinChanged -> setState { copy(proteinG = intent.value, errorMessage = null) }
            is MealLogIntent.CarbsChanged -> setState { copy(carbsG = intent.value, errorMessage = null) }
            is MealLogIntent.FatChanged -> setState { copy(fatG = intent.value, errorMessage = null) }
            is MealLogIntent.SlotSelected -> setState { copy(slot = intent.slot) }
            is MealLogIntent.ApplyAnalysis -> setState {
                copy(
                    mealName = intent.mealName,
                    calories = intent.calories.toString(),
                    proteinG = intent.proteinG.toFieldText(),
                    carbsG = intent.carbsG.toFieldText(),
                    fatG = intent.fatG.toFieldText(),
                )
            }
            MealLogIntent.Save -> save()
            MealLogIntent.Delete -> delete()
        }
    }

    private fun load(mealId: String?, slot: MealSlot?, session: Int) {
        if (loadedKey == Triple(mealId, slot, session)) return
        loadedKey = Triple(mealId, slot, session)
        if (mealId == null) {
            setState { MealLogState(slot = slot ?: MealSlot.Lunch) }
            return
        }
        viewModelScope.launch {
            when (val result = getMealEntryUseCase(mealId)) {
                is UseCaseState.Success -> {
                    val meal = result.data
                    setState {
                        MealLogState(
                            editingId = meal.id,
                            mealName = meal.mealName,
                            calories = meal.calories.toString(),
                            proteinG = meal.proteinG.toFieldText(),
                            carbsG = meal.carbsG.toFieldText(),
                            fatG = meal.fatG.toFieldText(),
                            slot = MealSlot.fromCode(meal.slot) ?: slot ?: MealSlot.Lunch,
                        )
                    }
                }
                is UseCaseState.Error -> setState { copy(errorMessage = result.message) }
            }
        }
    }

    private fun save() {
        val s = currentState()
        if (s.saving) return
        val calories = s.calories.toIntOrNull()
        // Quick add needs a name and calories; macros left empty count as 0, a typed one must be a number.
        val protein = s.proteinG.toOptionalMacroGrams()
        val carbs = s.carbsG.toOptionalMacroGrams()
        val fat = s.fatG.toOptionalMacroGrams()
        if (s.mealName.isBlank() || calories == null || protein == null || carbs == null || fat == null) {
            setState { copy(errorMessage = "validation_fields_required") }
            return
        }
        viewModelScope.launch {
            setState { copy(saving = true, errorMessage = null) }
            val result = if (s.editingId != null) {
                editMealEntryUseCase(s.editingId, s.mealName, calories, protein, carbs, fat, s.slot)
            } else {
                logMealUseCase(s.mealName.trim(), calories, protein, carbs, fat, slot = s.slot.code)
            }
            when (result) {
                is UseCaseState.Success -> {
                    setState { copy(saving = false) }
                    setEffect(NutritionEffect.MealSaved)
                }
                is UseCaseState.Error -> setState { copy(saving = false, errorMessage = result.message) }
            }
        }
    }

    private fun delete() {
        val id = currentState().editingId ?: return
        viewModelScope.launch {
            setState { copy(saving = true) }
            when (deleteMealEntryUseCase(id)) {
                is UseCaseState.Success -> setEffect(NutritionEffect.MealSaved)
                is UseCaseState.Error -> setState { copy(saving = false, errorMessage = "delete_failed") }
            }
        }
    }
}
