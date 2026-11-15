package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.CompleteSetUseCase
import com.fitai.app.domain.GenerateWorkoutPlanUseCase
import com.fitai.app.domain.StartWorkoutSessionUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.SessionSet
import com.fitai.app.training.GetSetSuggestionsUseCase
import com.fitai.app.training.SetSuggestion
import com.fitai.app.util.currentDayOfWeek
import com.fitai.app.util.epochMillis
import com.fitai.app.viewmodel.workout.ActiveSessionIntent
import com.fitai.app.viewmodel.workout.ActiveSessionState
import com.fitai.app.viewmodel.workout.ExerciseDetailEffect
import com.fitai.app.viewmodel.workout.ExerciseDetailIntent
import com.fitai.app.viewmodel.workout.ExerciseDetailState
import com.fitai.app.viewmodel.workout.ReplaceSheetState
import com.fitai.app.viewmodel.workout.RestTimerState
import com.fitai.app.viewmodel.workout.SetInput
import com.fitai.app.viewmodel.workout.WorkoutEffect
import com.fitai.app.viewmodel.workout.WorkoutIntent
import com.fitai.app.viewmodel.workout.WorkoutState
import com.fitai.app.viewmodel.workout.formatKg
import com.fitai.app.viewmodel.workout.parseKg
import com.fitai.app.workout.FinishWorkoutSessionUseCase
import com.fitai.app.workout.GetExerciseAlternativesUseCase
import com.fitai.app.workout.GetExerciseByIdUseCase
import com.fitai.app.workout.GetExercisesForDayUseCase
import com.fitai.app.workout.LoadActiveSessionUseCase
import com.fitai.app.workout.ObserveWorkoutOverviewUseCase
import com.fitai.app.workout.RefreshSessionSetsUseCase
import com.fitai.app.workout.ReplaceExerciseUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val observeWorkoutOverviewUseCase: ObserveWorkoutOverviewUseCase,
    private val getExercisesForDayUseCase: GetExercisesForDayUseCase,
    private val generateWorkoutPlanUseCase: GenerateWorkoutPlanUseCase,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<WorkoutState, WorkoutIntent, WorkoutEffect>(
    WorkoutState(selectedDay = currentDayOfWeek()),
) {
    private var observeJob: Job? = null

    init {
        refresh()
    }

    override fun handleIntent(intent: WorkoutIntent) {
        when (intent) {
            is WorkoutIntent.SelectDay -> selectDay(intent.day)
            WorkoutIntent.RegeneratePlan -> regenerate()
            WorkoutIntent.StartSession -> startSession()
            WorkoutIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val day = currentState().selectedDay
            observeWorkoutOverviewUseCase(day).collect { overview ->
                setState {
                    copy(
                        plans = overview.plans,
                        activePlan = overview.activePlan,
                        dayExercises = overview.dayExercises,
                        loading = false,
                    )
                }
            }
        }
    }

    private fun selectDay(day: Int) {
        viewModelScope.launch {
            val active = currentState().activePlan
            if (active == null) {
                setState { copy(selectedDay = day) }
                return@launch
            }
            when (val result = getExercisesForDayUseCase(active.id, day)) {
                is UseCaseState.Success ->
                    setState { copy(selectedDay = day, dayExercises = result.data) }
                is UseCaseState.Error ->
                    setState { copy(selectedDay = day, errorMessage = result.message) }
            }
        }
    }

    private fun regenerate() {
        viewModelScope.launch {
            setState { copy(generating = true, errorMessage = null) }
            when (val result = generateWorkoutPlanUseCase()) {
                is UseCaseState.Success -> setState { copy(generating = false) }
                is UseCaseState.Error -> {
                    if (result.message == "trial_expired") {
                        setEffect(WorkoutEffect.NavigateToPaywall)
                    }
                    setState { copy(generating = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun startSession() {
        viewModelScope.launch {
            when (val result = startWorkoutSessionUseCase(currentState().selectedDay)) {
                is UseCaseState.Success -> {
                    trackEventUseCase(AnalyticsEvents.WORKOUT_STARTED, mapOf("source" to "plan"))
                    setEffect(WorkoutEffect.NavigateToActiveSession(result.data.id))
                }
                is UseCaseState.Error ->
                    setState { copy(errorMessage = result.message) }
            }
        }
    }
}

class ExerciseDetailViewModel(
    private val getExerciseByIdUseCase: GetExerciseByIdUseCase,
    private val getExerciseAlternativesUseCase: GetExerciseAlternativesUseCase,
    private val replaceExerciseUseCase: ReplaceExerciseUseCase,
) : BaseViewModel<ExerciseDetailState, ExerciseDetailIntent, ExerciseDetailEffect>(
    ExerciseDetailState(),
) {
    override fun handleIntent(intent: ExerciseDetailIntent) {
        when (intent) {
            is ExerciseDetailIntent.Load -> viewModelScope.launch {
                setState { copy(loading = true, replaceSheet = null) }
                when (val result = getExerciseByIdUseCase(intent.exerciseId)) {
                    is UseCaseState.Success ->
                        setState { copy(exercise = result.data, loading = false) }
                    is UseCaseState.Error ->
                        setState { copy(loading = false) }
                }
            }
            ExerciseDetailIntent.OpenReplace -> openReplace()
            ExerciseDetailIntent.DismissReplace -> setState { copy(replaceSheet = null) }
            is ExerciseDetailIntent.ReplaceWith -> replaceWith(intent.name)
        }
    }

    fun load(exerciseId: String) {
        onIntent(ExerciseDetailIntent.Load(exerciseId))
    }

    private fun openReplace() {
        val exercise = currentState().exercise ?: return
        setState { copy(replaceSheet = ReplaceSheetState(exerciseId = exercise.id)) }
        viewModelScope.launch {
            val alternatives = (getExerciseAlternativesUseCase(exercise.id) as? UseCaseState.Success)?.data.orEmpty()
            setState { copy(replaceSheet = replaceSheet?.copy(alternatives = alternatives, loading = false)) }
        }
    }

    private fun replaceWith(name: String) {
        val sheet = currentState().replaceSheet ?: return
        if (sheet.replacing) return
        setState { copy(replaceSheet = sheet.copy(replacing = true)) }
        viewModelScope.launch {
            when (val result = replaceExerciseUseCase(sheet.exerciseId, name)) {
                is UseCaseState.Success -> setState { copy(exercise = result.data, replaceSheet = null) }
                is UseCaseState.Error -> setState { copy(replaceSheet = replaceSheet?.copy(replacing = false)) }
            }
        }
    }
}

class ActiveSessionViewModel(
    private val loadActiveSessionUseCase: LoadActiveSessionUseCase,
    private val completeSetUseCase: CompleteSetUseCase,
    private val refreshSessionSetsUseCase: RefreshSessionSetsUseCase,
    private val finishWorkoutSessionUseCase: FinishWorkoutSessionUseCase,
    private val getSetSuggestionsUseCase: GetSetSuggestionsUseCase,
    private val getExerciseAlternativesUseCase: GetExerciseAlternativesUseCase,
    private val replaceExerciseUseCase: ReplaceExerciseUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<ActiveSessionState, ActiveSessionIntent, WorkoutEffect>(ActiveSessionState()) {

    private var restJob: Job? = null
    private var restEndsAtMs: Long = 0L

    /** Re-entering the same session keeps typed inputs and a running rest timer. */
    fun load(sessionId: String) {
        val sameSession = currentState().session?.id == sessionId
        if (!sameSession) {
            stopRest()
            setState { ActiveSessionState(loading = true) }
        }
        viewModelScope.launch {
            val snapshot = (loadActiveSessionUseCase(sessionId) as? UseCaseState.Success)?.data
            if (snapshot == null) {
                setState { copy(loading = false) }
                return@launch
            }
            val suggestions = (getSetSuggestionsUseCase(sessionId) as? UseCaseState.Success)?.data.orEmpty()
            setState {
                copy(
                    session = snapshot.session,
                    plan = snapshot.plan,
                    sessionExercises = snapshot.sessionExercises,
                    exerciseDetails = snapshot.exerciseDetails,
                    setsBySessionExercise = snapshot.setsBySessionExercise,
                    suggestions = suggestions,
                    inputs = buildInputs(snapshot.setsBySessionExercise, suggestions, inputs),
                    loading = false,
                )
            }
        }
    }

    override fun handleIntent(intent: ActiveSessionIntent) {
        when (intent) {
            is ActiveSessionIntent.UpdateReps -> updateInput(intent.setId) { copy(reps = intent.value) }
            is ActiveSessionIntent.UpdateWeight -> updateInput(intent.setId) { copy(weight = intent.value) }
            is ActiveSessionIntent.CopyPreviousSet -> copyPreviousSet(intent.setId)
            is ActiveSessionIntent.CompleteSet -> completeSet(intent.setId)
            ActiveSessionIntent.SkipRest -> stopRest()
            is ActiveSessionIntent.AddRest -> addRest(intent.seconds)
            is ActiveSessionIntent.OpenReplace -> openReplace(intent.sessionExerciseId)
            ActiveSessionIntent.DismissReplace -> setState { copy(replaceSheet = null) }
            is ActiveSessionIntent.ReplaceWith -> replaceWith(intent.name)
            ActiveSessionIntent.FinishSession -> finishSession()
        }
    }

    private fun buildInputs(
        setsBySessionExercise: Map<String, List<SessionSet>>,
        suggestions: Map<String, SetSuggestion>,
        existing: Map<String, SetInput>,
    ): Map<String, SetInput> = buildMap {
        setsBySessionExercise.forEach { (sessionExerciseId, sets) ->
            val suggestion = suggestions[sessionExerciseId]
            sets.forEach { set ->
                put(
                    set.id,
                    when {
                        set.completed -> SetInput(set.actualReps.orEmpty(), set.weightKg?.let(::formatKg).orEmpty())
                        existing[set.id] != null -> existing.getValue(set.id)
                        else -> SetInput(
                            reps = suggestion?.reps?.takeIf { it > 0 }?.toString().orEmpty(),
                            weight = suggestion?.weightKg?.let(::formatKg).orEmpty(),
                        )
                    },
                )
            }
        }
    }

    private fun updateInput(setId: String, reducer: SetInput.() -> SetInput) {
        setState { copy(inputs = inputs + (setId to (inputs[setId] ?: SetInput()).reducer())) }
    }

    private fun copyPreviousSet(setId: String) {
        val state = currentState()
        val sets = state.setsBySessionExercise.values.firstOrNull { sets -> sets.any { it.id == setId } } ?: return
        val set = sets.first { it.id == setId }
        val previous = sets.lastOrNull { it.setNumber < set.setNumber } ?: return
        val source = state.inputs[previous.id] ?: return
        setState { copy(inputs = inputs + (setId to source)) }
    }

    private fun completeSet(setId: String) {
        val state = currentState()
        val sessionExerciseId = state.setsBySessionExercise.entries
            .firstOrNull { (_, sets) -> sets.any { it.id == setId } }
            ?.key ?: return
        val set = state.setsBySessionExercise.getValue(sessionExerciseId).first { it.id == setId }
        val input = state.inputs[setId] ?: SetInput()
        val weight = parseKg(input.weight)
        viewModelScope.launch {
            val result = completeSetUseCase(setId, input.reps.trim().ifBlank { set.targetReps }, weight)
            if (result is UseCaseState.Error) return@launch
            val exercise = state.sessionExercises.firstOrNull { it.id == sessionExerciseId }
                ?.let { state.exerciseDetails[it.exerciseId] }
            trackEventUseCase(
                AnalyticsEvents.SET_COMPLETED,
                mapOf(
                    "exercise" to exercise?.name.orEmpty(),
                    "set_number" to set.setNumber.toString(),
                    "weighted" to (weight != null).toString(),
                ),
            )
            when (val refreshed = refreshSessionSetsUseCase(sessionExerciseId)) {
                is UseCaseState.Success -> setState {
                    val sets = setsBySessionExercise + (sessionExerciseId to refreshed.data)
                    copy(setsBySessionExercise = sets, inputs = buildInputs(sets, suggestions, inputs))
                }
                is UseCaseState.Error -> Unit
            }
            val anyOpenSet = currentState().setsBySessionExercise.values.flatten().any { !it.completed }
            if (anyOpenSet) startRest(exercise?.restSeconds ?: 0)
        }
    }

    private fun startRest(seconds: Int) {
        stopRest()
        if (seconds <= 0) return
        restEndsAtMs = epochMillis() + seconds * 1_000L
        setState { copy(rest = RestTimerState(totalSeconds = seconds, remainingSeconds = seconds)) }
        restJob = viewModelScope.launch {
            while (isActive) {
                val remainingMs = restEndsAtMs - epochMillis()
                if (remainingMs <= 0) {
                    setState { copy(rest = null) }
                    break
                }
                val remaining = ((remainingMs + 999) / 1_000).toInt()
                setState { copy(rest = rest?.copy(remainingSeconds = remaining)) }
                delay(250)
            }
        }
    }

    private fun addRest(seconds: Int) {
        val rest = currentState().rest ?: return
        restEndsAtMs += seconds * 1_000L
        setState {
            copy(rest = rest.copy(totalSeconds = rest.totalSeconds + seconds, remainingSeconds = rest.remainingSeconds + seconds))
        }
    }

    private fun stopRest() {
        restJob?.cancel()
        restJob = null
        if (currentState().rest != null) setState { copy(rest = null) }
    }

    private fun openReplace(sessionExerciseId: String) {
        val exerciseId = currentState().sessionExercises.firstOrNull { it.id == sessionExerciseId }?.exerciseId ?: return
        setState { copy(replaceSheet = ReplaceSheetState(exerciseId = exerciseId, sessionExerciseId = sessionExerciseId)) }
        viewModelScope.launch {
            val alternatives = (getExerciseAlternativesUseCase(exerciseId) as? UseCaseState.Success)?.data.orEmpty()
            setState { copy(replaceSheet = replaceSheet?.copy(alternatives = alternatives, loading = false)) }
        }
    }

    private fun replaceWith(name: String) {
        val sheet = currentState().replaceSheet ?: return
        val sessionId = currentState().session?.id ?: return
        if (sheet.replacing) return
        setState { copy(replaceSheet = sheet.copy(replacing = true)) }
        viewModelScope.launch {
            when (replaceExerciseUseCase(sheet.exerciseId, name, sheet.sessionExerciseId)) {
                is UseCaseState.Success -> {
                    val staleSetIds = currentState().setsBySessionExercise[sheet.sessionExerciseId].orEmpty().map { it.id }
                    setState { copy(replaceSheet = null, inputs = inputs - staleSetIds.toSet()) }
                    load(sessionId)
                }
                is UseCaseState.Error -> setState { copy(replaceSheet = replaceSheet?.copy(replacing = false)) }
            }
        }
    }

    private fun finishSession() {
        val state = currentState()
        val session = state.session ?: return
        if (state.completing) return
        viewModelScope.launch {
            setState { copy(completing = true) }
            when (finishWorkoutSessionUseCase(session.id)) {
                is UseCaseState.Success -> {
                    stopRest()
                    val sets = currentState().setsBySessionExercise.values.flatten()
                    trackEventUseCase(
                        AnalyticsEvents.WORKOUT_COMPLETED,
                        mapOf(
                            "completed_sets" to sets.count { it.completed }.toString(),
                            "total_sets" to sets.size.toString(),
                            "source" to if (currentState().plan?.isAdHoc == true) "today_mode" else "plan",
                        ),
                    )
                    setState { copy(completing = false) }
                    setEffect(WorkoutEffect.SessionCompleted(session.id))
                }
                is UseCaseState.Error -> setState { copy(completing = false) }
            }
        }
    }
}
