package com.fitai.app.home

import com.fitai.app.domain.CheckTrialAccessUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.Exercise
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.trial.StartRequestedTrialUseCase
import com.fitai.app.trial.TrialRepository
import com.fitai.app.util.currentDayOfWeek

data class HomeDashboard(
    val userName: String,
    val activePlan: WorkoutPlan?,
    val todayExercises: List<Exercise>,
    /** Only a running or finished trial; nothing for accounts that never had one. */
    val trialStatus: TrialStatus?,
)

class GetHomeDashboardUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val trialRepository: TrialRepository,
    private val startRequestedTrialUseCase: StartRequestedTrialUseCase,
) {
    suspend operator fun invoke(): UseCaseState<HomeDashboard?> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: return@useCaseCatching null
        val plan = workoutRepository.getActivePlan(user.id)
        val exercises = plan?.let { workoutRepository.getExercisesForPlanDay(it.id, currentDayOfWeek()) }.orEmpty()
        HomeDashboard(
            userName = user.name,
            activePlan = plan,
            todayExercises = exercises,
            trialStatus = trialStatus(),
        )
    }

    private suspend fun trialStatus(): TrialStatus? {
        // A trial asked for on the paywall while offline (or before an account existed) starts once possible.
        startRequestedTrialUseCase()
        return trialRepository.getTrialStatus().getOrNull()
            ?.takeIf { it.state == TrialState.Active || it.state == TrialState.Expired }
    }
}

class CanAccessQuickActionsUseCase(
    private val checkTrialAccessUseCase: CheckTrialAccessUseCase,
) {
    suspend operator fun invoke(): UseCaseState<Boolean> = checkTrialAccessUseCase()
}
