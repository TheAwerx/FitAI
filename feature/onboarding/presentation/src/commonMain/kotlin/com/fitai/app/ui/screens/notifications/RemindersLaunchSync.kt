package com.fitai.app.ui.screens.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.notifications.reminderTexts
import com.fitai.app.viewmodel.RemindersLaunchSyncViewModel
import com.fitai.app.viewmodel.notifications.RemindersLaunchSyncIntent
import org.koin.compose.viewmodel.koinViewModel

/** Place once at the app root; rebuilds pending reminders with the current language once per launch. */
@Composable
fun RemindersLaunchSync(
    enabled: Boolean,
    viewModel: RemindersLaunchSyncViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    LaunchedEffect(enabled, strings) {
        if (enabled) viewModel.onIntent(RemindersLaunchSyncIntent.Sync(strings.reminderTexts()))
    }
}
