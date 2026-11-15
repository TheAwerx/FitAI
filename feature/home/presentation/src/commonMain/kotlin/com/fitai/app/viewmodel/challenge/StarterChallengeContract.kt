package com.fitai.app.viewmodel.challenge

import com.fitai.app.engagement.StarterChallenge
import com.fitai.app.engagement.StarterTaskId
import com.fitai.app.notifications.ReminderTexts

data class StarterChallengeState(
    val challenge: StarterChallenge? = null,
    val sheetOpen: Boolean = false,
) {
    val visible: Boolean get() = challenge?.visible == true
}

sealed interface StarterChallengeIntent {
    data object Refresh : StarterChallengeIntent
    data object OpenSheet : StarterChallengeIntent
    data object CloseSheet : StarterChallengeIntent
    data class TaskClicked(val id: StarterTaskId) : StarterChallengeIntent
    data class NotificationsResolved(val granted: Boolean, val texts: ReminderTexts) : StarterChallengeIntent
    data object OpenGift : StarterChallengeIntent
    data object HideForever : StarterChallengeIntent
}

sealed interface StarterChallengeEffect {
    data object RequestNotifications : StarterChallengeEffect
    data object OpenWorkout : StarterChallengeEffect
    data object OpenMealPhoto : StarterChallengeEffect
    data object OpenWeighIn : StarterChallengeEffect
    data object OpenGift : StarterChallengeEffect
}
