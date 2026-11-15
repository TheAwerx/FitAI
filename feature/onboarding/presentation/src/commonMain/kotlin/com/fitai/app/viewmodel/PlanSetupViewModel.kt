package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.AccessTier
import com.fitai.app.access.GetAccessTierUseCase
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.ReminderTexts
import com.fitai.app.onboarding.SetupInitialPlansUseCase
import com.fitai.app.viewmodel.plansetup.PlanSetupEffect
import com.fitai.app.viewmodel.plansetup.PlanSetupIntent
import com.fitai.app.viewmodel.plansetup.PlanSetupState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Assigns the first plans after sign-up, then schedules reminders against the new plan days. */
class PlanSetupViewModel(
    private val getAccessTierUseCase: GetAccessTierUseCase,
    private val setupInitialPlansUseCase: SetupInitialPlansUseCase,
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<PlanSetupState, PlanSetupIntent, PlanSetupEffect>(PlanSetupState()) {

    private var texts: ReminderTexts? = null

    override fun handleIntent(intent: PlanSetupIntent) {
        when (intent) {
            is PlanSetupIntent.Start -> {
                if (texts != null) return
                texts = intent.texts
                run(forceStarter = false)
            }
            PlanSetupIntent.Retry -> run(forceStarter = false)
            PlanSetupIntent.UseStarterPlan -> run(forceStarter = true)
        }
    }

    private fun run(forceStarter: Boolean) {
        if (currentState().running) return
        viewModelScope.launch {
            val personal = !forceStarter &&
                (getAccessTierUseCase() as? UseCaseState.Success)?.data == AccessTier.Pro
            setState { copy(personal = personal, running = true, failed = false, progress = 0.05f) }
            val result = setupInitialPlansUseCase(forceStarter = forceStarter) { progress ->
                setState { copy(progress = progress * PLAN_SHARE) }
                delay(STEP_PAUSE_MS)
            }
            when (result) {
                is UseCaseState.Error -> setState { copy(running = false, failed = true) }
                is UseCaseState.Success -> {
                    texts?.let { buildAndScheduleRemindersUseCase(it) }
                    setState { copy(progress = 1f) }
                    trackEventUseCase(AnalyticsEvents.PLAN_ASSIGNED, mapOf(PARAM_KIND to result.data.name.lowercase()))
                    delay(STEP_PAUSE_MS)
                    setState { copy(running = false) }
                    setEffect(PlanSetupEffect.Done)
                }
            }
        }
    }

    private companion object {
        const val PARAM_KIND = "kind"
        const val PLAN_SHARE = 0.85f
        const val STEP_PAUSE_MS = 450L
    }
}
