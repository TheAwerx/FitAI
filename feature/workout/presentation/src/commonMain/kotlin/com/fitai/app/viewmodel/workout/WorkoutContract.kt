package com.fitai.app.viewmodel.workout

import com.fitai.app.access.FeatureAccess
import com.fitai.app.access.PlanQuota
import com.fitai.app.model.Exercise
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.TodayLocation
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import com.fitai.app.training.PersonalRecord
import com.fitai.app.training.SetSuggestion
import com.fitai.app.ui.plangeneration.PlanGenerationPhase
import com.fitai.app.ui.plangeneration.PlanGenerationUiState
import com.fitai.app.workout.ExerciseAlternativeOption
import com.fitai.app.workout.SessionSummary

data class WorkoutState(
    val plans: List<WorkoutPlan> = emptyList(),
    val activePlan: WorkoutPlan? = null,
    val selectedDay: Int = com.fitai.app.util.currentDayOfWeek(),
    val dayExercises: List<Exercise> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    /** Regeneration rights; null until the first read. */
    val planQuota: PlanQuota? = null,
    val confirmRegenerate: Boolean = false,
    /** Non-null while the generation overlay is on screen. */
    val generation: PlanGenerationUiState? = null,
) {
    val generating: Boolean get() = generation?.phase == PlanGenerationPhase.Running
}

/** Replace sheet state shared by exercise detail and the active session. */
data class ReplaceSheetState(
    val exerciseId: String,
    val sessionExerciseId: String? = null,
    val alternatives: List<ExerciseAlternativeOption> = emptyList(),
    val loading: Boolean = true,
    val replacing: Boolean = false,
)

data class ExerciseDetailState(
    val exercise: Exercise? = null,
    val loading: Boolean = false,
    val replaceSheet: ReplaceSheetState? = null,
)

data class SetInput(
    val reps: String = "",
    val weight: String = "",
)

data class RestTimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
)

data class ActiveSessionState(
    val session: WorkoutSession? = null,
    val plan: WorkoutPlan? = null,
    val sessionExercises: List<SessionExercise> = emptyList(),
    val exerciseDetails: Map<String, Exercise> = emptyMap(),
    val setsBySessionExercise: Map<String, List<SessionSet>> = emptyMap(),
    /** Keyed by session exercise id. */
    val suggestions: Map<String, SetSuggestion> = emptyMap(),
    /** Keyed by set id. */
    val inputs: Map<String, SetInput> = emptyMap(),
    val rest: RestTimerState? = null,
    val replaceSheet: ReplaceSheetState? = null,
    val completing: Boolean = false,
    val loading: Boolean = false,
)

enum class TodayPainArea(val code: String) {
    Knee("knee"),
    LowerBack("lower_back"),
    Shoulder("shoulder"),
    Wrist("wrist"),
}

data class TodayModeState(
    val access: FeatureAccess? = null,
    val checkingAccess: Boolean = true,
    val minutes: Int = 30,
    val energy: Int = 3,
    val painAreas: Set<TodayPainArea> = emptySet(),
    val location: String = TodayLocation.GYM,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        val MINUTE_OPTIONS = listOf(20, 30, 45, 60)
        val LOCATIONS = listOf(TodayLocation.GYM, TodayLocation.HOME, TodayLocation.HOTEL, TodayLocation.OUTDOOR)
    }
}

data class SessionSummaryState(
    val summary: SessionSummary? = null,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val loading: Boolean = true,
)

sealed interface WorkoutIntent {
    data class SelectDay(val day: Int) : WorkoutIntent
    /** "Planı yenile": asks for confirmation, or builds the first plan when there is none. */
    data object RegeneratePlan : WorkoutIntent
    data object ConfirmRegenerate : WorkoutIntent
    data object DismissRegenerate : WorkoutIntent
    data object RetryGeneration : WorkoutIntent
    data object CloseGeneration : WorkoutIntent
    data object StartSession : WorkoutIntent
    data object Refresh : WorkoutIntent
}

sealed interface ActiveSessionIntent {
    data class UpdateReps(val setId: String, val value: String) : ActiveSessionIntent
    data class UpdateWeight(val setId: String, val value: String) : ActiveSessionIntent
    data class CopyPreviousSet(val setId: String) : ActiveSessionIntent
    data class CompleteSet(val setId: String) : ActiveSessionIntent
    data object SkipRest : ActiveSessionIntent
    data class AddRest(val seconds: Int) : ActiveSessionIntent
    data class OpenReplace(val sessionExerciseId: String) : ActiveSessionIntent
    data object DismissReplace : ActiveSessionIntent
    data class ReplaceWith(val name: String) : ActiveSessionIntent
    data object FinishSession : ActiveSessionIntent
}

sealed interface ExerciseDetailIntent {
    data class Load(val exerciseId: String) : ExerciseDetailIntent
    data object OpenReplace : ExerciseDetailIntent
    data object DismissReplace : ExerciseDetailIntent
    data class ReplaceWith(val name: String) : ExerciseDetailIntent
}

sealed interface TodayModeIntent {
    data object Load : TodayModeIntent
    data class SelectMinutes(val minutes: Int) : TodayModeIntent
    data class SelectEnergy(val energy: Int) : TodayModeIntent
    data class TogglePain(val area: TodayPainArea) : TodayModeIntent
    data object ClearPain : TodayModeIntent
    data class SelectLocation(val location: String) : TodayModeIntent
    data object Submit : TodayModeIntent
}

sealed interface SessionSummaryIntent {
    data class Load(val sessionId: String) : SessionSummaryIntent
}

sealed interface WorkoutEffect {
    data class NavigateToExercise(val exerciseId: String) : WorkoutEffect
    data class NavigateToActiveSession(val sessionId: String) : WorkoutEffect
    data object NavigateToPaywall : WorkoutEffect
    data class SessionCompleted(val sessionId: String) : WorkoutEffect
}

sealed interface ExerciseDetailEffect

sealed interface SessionSummaryEffect
