package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.GatedFeature
import com.fitai.app.domain.GenerateDietPlanUseCase
import com.fitai.app.domain.LogMealUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.MealAnalysisResult
import com.fitai.app.nutrition.AnalyzeMealPhotoWithQuotaUseCase
import com.fitai.app.nutrition.GetTodayNutritionOverviewUseCase
import com.fitai.app.nutrition.LogAnalyzedMealUseCase
import com.fitai.app.nutrition.MealPhotoAnalysisOutcome
import com.fitai.app.nutrition.NextMealOutcome
import com.fitai.app.nutrition.ObserveNutritionOverviewUseCase
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
import kotlin.math.abs
import kotlin.math.roundToInt

class NutritionViewModel(
    private val observeNutritionOverviewUseCase: ObserveNutritionOverviewUseCase,
    private val getTodayNutritionOverviewUseCase: GetTodayNutritionOverviewUseCase,
    private val generateDietPlanUseCase: GenerateDietPlanUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val analyzeMealPhotoWithQuotaUseCase: AnalyzeMealPhotoWithQuotaUseCase,
    private val logAnalyzedMealUseCase: LogAnalyzedMealUseCase,
    private val suggestNextMealUseCase: SuggestNextMealUseCase,
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
            NutritionIntent.OpenManualLog -> setEffect(NutritionEffect.NavigateToMealLog)
            NutritionIntent.OpenPaywall -> setEffect(NutritionEffect.NavigateToPaywall)
            is NutritionIntent.PhotoPicked -> analyzePhoto(intent.bytes, intent.mimeType)
            NutritionIntent.PhotoPickCancelled -> Unit
            is NutritionIntent.ReviewChanged -> updateReview(intent.fields)
            NutritionIntent.SaveAnalyzedMeal -> saveAnalyzedMeal()
            NutritionIntent.DiscardAnalysis -> setState { copy(plate = PlatePhotoState.Idle) }
            NutritionIntent.RetryNextMeal -> suggestNextMeal()
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
                        loading = false,
                    )
                }
                val today = getTodayNutritionOverviewUseCase()
                if (today is UseCaseState.Success) setState { copy(today = today.data) }
            }
        }
        refreshPhotoAccess()
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
            when (logAnalyzedMealUseCase(fields.mealName.trim(), calories, protein, carbs, fat)) {
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
private fun Double.toFieldText(): String {
    val tenths = (coerceAtLeast(0.0) * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${abs(tenths % 10)}"
}

/** Accepts both "12.5" and the Turkish "12,5". */
private fun String.toMacroGrams(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

class MealLogViewModel(
    private val logMealUseCase: LogMealUseCase,
) : BaseViewModel<MealLogState, MealLogIntent, NutritionEffect>(MealLogState()) {

    override fun handleIntent(intent: MealLogIntent) {
        when (intent) {
            is MealLogIntent.MealNameChanged -> setState { copy(mealName = intent.value) }
            is MealLogIntent.CaloriesChanged ->
                setState { copy(calories = intent.value.filter(Char::isDigit)) }
            is MealLogIntent.ProteinChanged -> setState { copy(proteinG = intent.value) }
            is MealLogIntent.CarbsChanged -> setState { copy(carbsG = intent.value) }
            is MealLogIntent.FatChanged -> setState { copy(fatG = intent.value) }
            is MealLogIntent.ApplyAnalysis -> setState {
                copy(
                    mealName = intent.mealName,
                    calories = intent.calories.toString(),
                    proteinG = intent.proteinG.toString(),
                    carbsG = intent.carbsG.toString(),
                    fatG = intent.fatG.toString(),
                )
            }
            MealLogIntent.Save -> save()
        }
    }

    private fun save() {
        val s = currentState()
        val calories = s.calories.toIntOrNull()
        val protein = s.proteinG.toMacroGrams()
        val carbs = s.carbsG.toMacroGrams()
        val fat = s.fatG.toMacroGrams()
        if (s.mealName.isBlank() || calories == null || protein == null || carbs == null || fat == null) {
            setState { copy(errorMessage = "validation_fields_required") }
            return
        }
        viewModelScope.launch {
            setState { copy(saving = true, errorMessage = null) }
            when (val result = logMealUseCase(s.mealName, calories, protein, carbs, fat)) {
                is UseCaseState.Success -> {
                    setState { copy(saving = false) }
                    setEffect(NutritionEffect.MealSaved)
                }
                is UseCaseState.Error ->
                    setState { copy(saving = false, errorMessage = result.message) }
            }
        }
    }
}
