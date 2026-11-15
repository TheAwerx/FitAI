package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.viewmodel.notifications.RemindersLaunchSyncIntent
import com.fitai.app.viewmodel.notifications.RemindersLaunchSyncState
import kotlinx.coroutines.launch

/** Rebuilds reminders once per process so texts follow the current language and plan. */
class RemindersLaunchSyncViewModel(
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
) : BaseViewModel<RemindersLaunchSyncState, RemindersLaunchSyncIntent, Unit>(RemindersLaunchSyncState()) {

    override fun handleIntent(intent: RemindersLaunchSyncIntent) {
        when (intent) {
            is RemindersLaunchSyncIntent.Sync -> {
                if (currentState().synced) return
                setState { copy(synced = true) }
                viewModelScope.launch { buildAndScheduleRemindersUseCase(intent.texts) }
            }
        }
    }
}
