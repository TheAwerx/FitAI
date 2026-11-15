package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.GetPushNotificationTypesUseCase
import com.fitai.app.notifications.IsServerPushActiveUseCase
import com.fitai.app.notifications.SetPushNotificationTypeUseCase
import com.fitai.app.notifications.with
import com.fitai.app.viewmodel.notifications.PushNotificationSettingsIntent
import com.fitai.app.viewmodel.notifications.PushNotificationSettingsState
import kotlinx.coroutines.launch

/** Settings → Smart notifications: per-type switches for server pushes (synced to `users/{uid}`). */
class PushNotificationSettingsViewModel(
    private val getPushNotificationTypesUseCase: GetPushNotificationTypesUseCase,
    private val setPushNotificationTypeUseCase: SetPushNotificationTypeUseCase,
    private val isServerPushActiveUseCase: IsServerPushActiveUseCase,
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
) : BaseViewModel<PushNotificationSettingsState, PushNotificationSettingsIntent, Unit>(PushNotificationSettingsState()) {

    init {
        refresh()
    }

    override fun handleIntent(intent: PushNotificationSettingsIntent) {
        when (intent) {
            PushNotificationSettingsIntent.Refresh -> refresh()
            is PushNotificationSettingsIntent.Toggle -> toggle(intent)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val types = (getPushNotificationTypesUseCase() as? UseCaseState.Success)?.data
            val active = (isServerPushActiveUseCase() as? UseCaseState.Success)?.data
            setState { copy(types = types ?: this.types, pushActive = active) }
        }
    }

    private fun toggle(intent: PushNotificationSettingsIntent.Toggle) {
        if (currentState().busy) return
        setState { copy(types = types.with(intent.type, intent.enabled), busy = true) }
        viewModelScope.launch {
            val saved = setPushNotificationTypeUseCase(intent.type, intent.enabled)
            // The local meal reminder follows the meal switch while server pushes aren't active.
            buildAndScheduleRemindersUseCase(intent.texts)
            val active = (isServerPushActiveUseCase() as? UseCaseState.Success)?.data
            setState {
                copy(
                    types = (saved as? UseCaseState.Success)?.data ?: types.with(intent.type, !intent.enabled),
                    pushActive = active ?: pushActive,
                    busy = false,
                )
            }
        }
    }
}
