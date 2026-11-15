package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.engagement.GetStarterChallengeUseCase
import com.fitai.app.engagement.HideStarterChallengeUseCase
import com.fitai.app.engagement.StarterTaskId
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.SetRemindersEnabledUseCase
import com.fitai.app.viewmodel.challenge.StarterChallengeEffect
import com.fitai.app.viewmodel.challenge.StarterChallengeIntent
import com.fitai.app.viewmodel.challenge.StarterChallengeState
import kotlinx.coroutines.launch

/** Floating first-week goals shown above the tab bar on every main screen. */
class StarterChallengeViewModel(
    private val getStarterChallengeUseCase: GetStarterChallengeUseCase,
    private val hideStarterChallengeUseCase: HideStarterChallengeUseCase,
    private val setRemindersEnabledUseCase: SetRemindersEnabledUseCase,
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<StarterChallengeState, StarterChallengeIntent, StarterChallengeEffect>(StarterChallengeState()) {

    override fun handleIntent(intent: StarterChallengeIntent) {
        when (intent) {
            StarterChallengeIntent.Refresh -> refresh()
            StarterChallengeIntent.OpenSheet -> {
                trackEventUseCase(AnalyticsEvents.STARTER_CHALLENGE_OPENED)
                setState { copy(sheetOpen = true) }
            }
            StarterChallengeIntent.CloseSheet -> setState { copy(sheetOpen = false) }
            is StarterChallengeIntent.TaskClicked -> {
                setState { copy(sheetOpen = false) }
                setEffect(
                    when (intent.id) {
                        StarterTaskId.Notifications -> StarterChallengeEffect.RequestNotifications
                        StarterTaskId.FirstWorkout -> StarterChallengeEffect.OpenWorkout
                        StarterTaskId.FirstMealPhoto -> StarterChallengeEffect.OpenMealPhoto
                        StarterTaskId.FirstWeighIn -> StarterChallengeEffect.OpenWeighIn
                    },
                )
            }
            is StarterChallengeIntent.NotificationsResolved -> viewModelScope.launch {
                setRemindersEnabledUseCase(intent.granted)
                if (intent.granted) buildAndScheduleRemindersUseCase(intent.texts)
                refresh()
            }
            StarterChallengeIntent.OpenGift -> {
                if (currentState().challenge?.allDone != true) return
                hideStarterChallengeUseCase(giftClaimed = true)
                setState { copy(sheetOpen = false) }
                setEffect(StarterChallengeEffect.OpenGift)
                refresh()
            }
            StarterChallengeIntent.HideForever -> {
                hideStarterChallengeUseCase(giftClaimed = false)
                setState { copy(sheetOpen = false) }
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val result = getStarterChallengeUseCase()
            if (result is UseCaseState.Success) setState { copy(challenge = result.data) }
        }
    }
}
