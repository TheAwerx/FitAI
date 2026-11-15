package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.ObservePushTokenRefreshesUseCase
import com.fitai.app.viewmodel.notifications.RemindersLaunchSyncIntent
import com.fitai.app.viewmodel.notifications.RemindersLaunchSyncState
import kotlinx.coroutines.launch

/**
 * Rebuilds reminders once per process so texts follow the current language and plan. The rebuild also
 * registers this device's push token; a token FCM issues later (rotation, iOS APNs arriving late)
 * triggers another rebuild, which re-registers it and drops the local meal reminder once pushes own it.
 */
class RemindersLaunchSyncViewModel(
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
    private val observePushTokenRefreshesUseCase: ObservePushTokenRefreshesUseCase,
) : BaseViewModel<RemindersLaunchSyncState, RemindersLaunchSyncIntent, Unit>(RemindersLaunchSyncState()) {

    override fun handleIntent(intent: RemindersLaunchSyncIntent) {
        when (intent) {
            is RemindersLaunchSyncIntent.Sync -> {
                if (currentState().synced) return
                setState { copy(synced = true) }
                viewModelScope.launch { buildAndScheduleRemindersUseCase(intent.texts) }
                viewModelScope.launch {
                    observePushTokenRefreshesUseCase().collect { buildAndScheduleRemindersUseCase(intent.texts) }
                }
            }
        }
    }
}
