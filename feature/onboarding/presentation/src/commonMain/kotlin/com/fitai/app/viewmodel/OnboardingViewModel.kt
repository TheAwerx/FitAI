package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.FocusArea
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.encodeProfileCodes
import com.fitai.app.model.toggleDietRestriction
import com.fitai.app.model.toggleEquipmentOption
import com.fitai.app.notifications.SetRemindersEnabledUseCase
import com.fitai.app.onboarding.ConfirmCommitmentUseCase
import com.fitai.app.onboarding.OnboardingProfileInput
import com.fitai.app.onboarding.SaveAttributionUseCase
import com.fitai.app.onboarding.SaveGuestProfileUseCase
import com.fitai.app.onboarding.SaveOnboardingAnswersUseCase
import com.fitai.app.onboarding.estimatedGoalDate
import com.fitai.app.viewmodel.onboarding.GoalDate
import com.fitai.app.viewmodel.onboarding.OnboardingEffect
import com.fitai.app.viewmodel.onboarding.OnboardingIntent
import com.fitai.app.viewmodel.onboarding.OnboardingPhase
import com.fitai.app.viewmodel.onboarding.OnboardingState
import com.fitai.app.viewmodel.onboarding.OnboardingStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.number

/**
 * Collects the profile and answers, then plays a short "building your plan" sequence.
 * No plan is generated here: plans are assigned after sign-up (AI for Pro, starter otherwise).
 */
class OnboardingViewModel(
    private val saveGuestProfileUseCase: SaveGuestProfileUseCase,
    private val saveOnboardingAnswersUseCase: SaveOnboardingAnswersUseCase,
    private val saveAttributionUseCase: SaveAttributionUseCase,
    private val confirmCommitmentUseCase: ConfirmCommitmentUseCase,
    private val setRemindersEnabledUseCase: SetRemindersEnabledUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<OnboardingState, OnboardingIntent, OnboardingEffect>(OnboardingState()) {

    private data class ProfileSnapshot(
        val name: String,
        val age: String,
        val heightCm: String,
        val weightKg: String,
        val goal: String,
        val activityLevel: String,
        val equipment: Set<String>,
        val dietRestrictions: Set<String>,
    )

    /** Profile already written to the device; an unchanged profile is not inserted again. */
    private var savedFor: ProfileSnapshot? = null
    private var trackedAttribution: String? = null
    private var sequenceJob: Job? = null

    init {
        viewModelScope.launch {
            state.map { it.phase to it.step }
                .distinctUntilChanged()
                .collect { (phase, step) ->
                    if (phase == OnboardingPhase.SaveError) return@collect
                    val name = if (phase == OnboardingPhase.Profile) step.analyticsName else phase.analyticsName
                    trackEventUseCase(AnalyticsEvents.ONBOARDING_STEP_VIEWED, mapOf(PARAM_STEP to name))
                }
        }
    }

    override fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.NameChanged -> setState { copy(name = intent.value, errorMessage = null) }
            is OnboardingIntent.AgeChanged -> setState { copy(age = intent.value.filter(Char::isDigit)) }
            is OnboardingIntent.HeightChanged ->
                setState { copy(heightCm = intent.value.filter { it.isDigit() || it == '.' }) }
            is OnboardingIntent.WeightChanged ->
                setState { copy(weightKg = intent.value.filter { it.isDigit() || it == '.' }) }
            is OnboardingIntent.GoalSelected -> setState { copy(goal = intent.code) }
            is OnboardingIntent.ObstacleSelected -> setState { copy(obstacle = intent.code) }
            is OnboardingIntent.FocusToggled -> toggleFocus(intent.code)
            is OnboardingIntent.ExperienceSelected -> setState { copy(experience = intent.code) }
            is OnboardingIntent.GenderSelected -> setState { copy(gender = intent.code) }
            is OnboardingIntent.ActivitySelected -> setState { copy(activityLevel = intent.code) }
            is OnboardingIntent.EquipmentToggled ->
                setState { copy(equipment = toggleEquipmentOption(equipment, intent.code)) }
            is OnboardingIntent.DaysSelected -> setState { copy(daysPerWeek = intent.days) }
            is OnboardingIntent.DurationSelected -> setState { copy(sessionMinutes = intent.minutes) }
            is OnboardingIntent.DietToggled ->
                setState { copy(dietRestrictions = toggleDietRestriction(dietRestrictions, intent.code)) }
            is OnboardingIntent.AttributionSelected -> setState { copy(attributionSource = intent.source) }
            is OnboardingIntent.StatementAnswered -> {
                trackEventUseCase(
                    AnalyticsEvents.ONBOARDING_STATEMENT_ANSWERED,
                    mapOf(PARAM_STEP to currentState().step.analyticsName, PARAM_AGREE to intent.agree.toString()),
                )
                nextStep()
            }
            is OnboardingIntent.NotificationsResolved -> {
                setRemindersEnabledUseCase(intent.granted)
                trackEventUseCase(
                    AnalyticsEvents.NOTIFICATION_PERMISSION_RESULT,
                    mapOf(PARAM_RESULT to if (intent.granted) "granted" else "denied", PARAM_SOURCE to SOURCE_ONBOARDING),
                )
                nextStep()
            }
            OnboardingIntent.NextStep -> nextStep()
            OnboardingIntent.PreviousStep -> previousStep()
            OnboardingIntent.RetrySave -> saveAndAnalyze()
            OnboardingIntent.ContinueFromResult -> {
                if (currentState().phase != OnboardingPhase.Result) return
                showQuote()
            }
            OnboardingIntent.ContinueFromQuote -> {
                if (currentState().phase != OnboardingPhase.Quote) return
                sequenceJob?.cancel()
                setState { copy(phase = OnboardingPhase.Pledge) }
            }
            OnboardingIntent.ConfirmPledge -> confirmPledge()
        }
    }

    private fun toggleFocus(code: String) {
        val current = currentState().focusAreas
        when {
            code in current -> setState { copy(focusAreas = current - code, focusLimitReached = false) }
            current.size >= FocusArea.MAX_SELECTED -> setState { copy(focusLimitReached = true) }
            else -> setState { copy(focusAreas = current + code, focusLimitReached = false) }
        }
    }

    private fun nextStep() {
        val s = currentState()
        if (s.phase != OnboardingPhase.Profile || !s.canContinue) return
        if (s.step == OnboardingStep.Attribution) saveAttributionIfSelected(s.attributionSource)
        val next = OnboardingStep.entries.getOrNull(s.step.ordinal + 1)
        if (next == null) {
            saveAndAnalyze()
        } else {
            setState { copy(step = next, errorMessage = null, focusLimitReached = false) }
        }
    }

    private fun previousStep() {
        val s = currentState()
        when (s.phase) {
            OnboardingPhase.Profile -> {
                val previous = OnboardingStep.entries.getOrNull(s.step.ordinal - 1)
                if (previous == null) {
                    setEffect(OnboardingEffect.NavigateBackToWelcome)
                } else {
                    setState { copy(step = previous, errorMessage = null, focusLimitReached = false) }
                }
            }
            OnboardingPhase.Analyzing -> Unit
            OnboardingPhase.SaveError, OnboardingPhase.Result -> {
                sequenceJob?.cancel()
                setState { copy(phase = OnboardingPhase.Profile, step = OnboardingStep.entries.last(), saving = false) }
            }
            OnboardingPhase.Quote, OnboardingPhase.Pledge -> {
                if (s.pledgeSigned) return
                sequenceJob?.cancel()
                setState { copy(phase = OnboardingPhase.Result) }
            }
        }
    }

    private fun saveAttributionIfSelected(source: String?) {
        if (source == null || source == trackedAttribution) return
        if (saveAttributionUseCase(source) is UseCaseState.Success) {
            trackedAttribution = source
            trackEventUseCase(AnalyticsEvents.ATTRIBUTION_SELECTED, mapOf(PARAM_SOURCE to source))
        }
    }

    private fun OnboardingState.toSnapshot() = ProfileSnapshot(
        name = name.trim(),
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        activityLevel = activityLevel,
        equipment = equipment,
        dietRestrictions = dietRestrictions,
    )

    private fun saveAndAnalyze() {
        val s = currentState()
        if (s.saving) return
        val age = s.age.toLongOrNull()
        val height = s.heightCm.toDoubleOrNull()
        val weight = s.weightKg.toDoubleOrNull()
        if (age == null || height == null || weight == null) {
            setState { copy(phase = OnboardingPhase.Profile, step = OnboardingStep.Age, errorMessage = VALIDATION_NUMBERS) }
            return
        }
        sequenceJob?.cancel()
        sequenceJob = viewModelScope.launch {
            setState { copy(saving = true, phase = OnboardingPhase.Analyzing, analyzingPercent = 0, errorMessage = null) }
            val answers = OnboardingAnswers(
                gender = s.gender,
                experience = s.experience,
                obstacle = s.obstacle,
                focusAreas = s.focusAreas,
                daysPerWeek = s.resolvedDays,
                sessionMinutes = s.resolvedMinutes,
            )
            val snapshot = s.toSnapshot()
            val saved = if (snapshot == savedFor) {
                UseCaseState.Success(Unit)
            } else {
                saveGuestProfileUseCase(
                    OnboardingProfileInput(
                        name = s.name,
                        age = age,
                        heightCm = height,
                        weightKg = weight,
                        goal = s.goal,
                        activityLevel = s.activityLevel,
                        equipment = encodeProfileCodes(s.equipment),
                        dietRestrictions = encodeProfileCodes(s.dietRestrictions),
                    ),
                )
            }
            val answersSaved = saveOnboardingAnswersUseCase(answers)
            val error = (saved as? UseCaseState.Error) ?: (answersSaved as? UseCaseState.Error)
            if (error != null) {
                setState { copy(saving = false, phase = OnboardingPhase.SaveError, errorMessage = error.message) }
                return@launch
            }
            savedFor = snapshot
            for (percent in 1..100) {
                delay(if (percent in SLOW_PERCENTS) SLOW_TICK_MS else TICK_MS)
                setState { copy(analyzingPercent = percent) }
            }
            val date = estimatedGoalDate(s.goal)
            setState {
                copy(
                    saving = false,
                    phase = OnboardingPhase.Result,
                    goalDate = GoalDate(date.day, date.month.number, date.year),
                )
            }
        }
    }

    private fun showQuote() {
        sequenceJob?.cancel()
        setState { copy(phase = OnboardingPhase.Quote) }
        sequenceJob = viewModelScope.launch {
            delay(QUOTE_DURATION_MS)
            if (currentState().phase == OnboardingPhase.Quote) setState { copy(phase = OnboardingPhase.Pledge) }
        }
    }

    private fun confirmPledge() {
        val s = currentState()
        if (s.phase != OnboardingPhase.Pledge || s.pledgeSigned) return
        when (val result = confirmCommitmentUseCase(s.resolvedDays)) {
            is UseCaseState.Error -> setState { copy(errorMessage = result.message) }
            is UseCaseState.Success -> {
                setState { copy(pledgeSigned = true, errorMessage = null) }
                trackEventUseCase(
                    AnalyticsEvents.COMMITMENT_CONFIRMED,
                    mapOf(PARAM_DAYS_PER_WEEK to s.resolvedDays.toString()),
                )
                trackEventUseCase(AnalyticsEvents.ONBOARDING_COMPLETED)
                viewModelScope.launch {
                    delay(SIGNED_PAUSE_MS)
                    setEffect(OnboardingEffect.NavigateToPaywall)
                }
            }
        }
    }

    private companion object {
        const val PARAM_STEP = "step"
        const val PARAM_SOURCE = "source"
        const val PARAM_RESULT = "result"
        const val PARAM_AGREE = "agree"
        const val PARAM_DAYS_PER_WEEK = "days_per_week"
        const val SOURCE_ONBOARDING = "onboarding"
        const val VALIDATION_NUMBERS = "validation_numbers_required"
        const val TICK_MS = 45L
        const val SLOW_TICK_MS = 140L
        val SLOW_PERCENTS = (60..72) + (93..99)
        const val QUOTE_DURATION_MS = 4200L
        const val SIGNED_PAUSE_MS = 700L
    }
}
