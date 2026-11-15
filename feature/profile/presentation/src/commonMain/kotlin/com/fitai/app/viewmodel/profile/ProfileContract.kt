package com.fitai.app.viewmodel.profile

import com.fitai.app.access.PlanQuota
import com.fitai.app.auth.AuthUser
import com.fitai.app.engagement.WorkoutHistory
import com.fitai.app.engagement.WorkoutStreak
import com.fitai.app.engagement.WorkoutTrendMetric
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.notifications.ReminderSettings
import com.fitai.app.notifications.ReminderTexts
import com.fitai.app.ui.plangeneration.PlanGenerationPhase
import com.fitai.app.ui.plangeneration.PlanGenerationUiState

enum class ProfileTab { Workouts, Charts }

data class ProfileState(
    val localUser: LocalUserProfile? = null,
    val authUser: AuthUser? = null,
    val message: String? = null,
    val isFirebaseAvailable: Boolean = false,
    val isPro: Boolean = false,
    val history: WorkoutHistory? = null,
    val streak: WorkoutStreak? = null,
    val tab: ProfileTab = ProfileTab.Workouts,
    val trendMetric: WorkoutTrendMetric = WorkoutTrendMetric.Volume,
    val trendWeeks: Int = TREND_PERIODS.first(),
    val editing: Boolean = false,
    val saving: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val editAge: String = "",
    val editHeightCm: String = "",
    val editWeightKg: String = "",
    val editGoal: String = "",
    val editActivityLevel: String = "",
    val editEquipment: Set<String> = emptySet(),
    val editDietRestrictions: Set<String> = setOf(DietRestriction.None.code),
    val reminders: ReminderSettings? = null,
    val remindersBusy: Boolean = false,
    val signingOut: Boolean = false,
    /** Workout plan regeneration rights, shared with the Workout tab. */
    val planQuota: PlanQuota? = null,
    val confirmRegenerate: Boolean = false,
    val generation: PlanGenerationUiState? = null,
) {
    val regenerating: Boolean get() = generation?.phase == PlanGenerationPhase.Running

    /** Signed in with a real account (not the anonymous session used before sign-up). */
    val hasAccount: Boolean get() = authUser?.isAnonymous == false

    companion object {
        val TREND_PERIODS = listOf(4, 12, 26)
    }
}

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data class SelectTab(val tab: ProfileTab) : ProfileIntent
    data class SelectTrendMetric(val metric: WorkoutTrendMetric) : ProfileIntent
    data class SelectTrendWeeks(val weeks: Int) : ProfileIntent
    data object SignOut : ProfileIntent
    data class GoogleIdToken(val token: String) : ProfileIntent
    data class AppleTokens(val idToken: String, val rawNonce: String) : ProfileIntent
    data class ProviderError(val message: String?) : ProfileIntent
    data object ConsumeMessage : ProfileIntent
    data object StartEdit : ProfileIntent
    data object CancelEdit : ProfileIntent
    data class EditName(val value: String) : ProfileIntent
    data class EditEmail(val value: String) : ProfileIntent
    data class EditAge(val value: String) : ProfileIntent
    data class EditHeight(val value: String) : ProfileIntent
    data class EditWeight(val value: String) : ProfileIntent
    data class EditGoal(val code: String) : ProfileIntent
    data class EditActivity(val code: String) : ProfileIntent
    data class ToggleEquipment(val code: String) : ProfileIntent
    data class ToggleDiet(val code: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object RegeneratePlans : ProfileIntent
    data object ConfirmRegeneratePlans : ProfileIntent
    data object DismissRegeneratePlans : ProfileIntent
    data object CloseRegeneratePlans : ProfileIntent
    data class RemindersPermissionResolved(val granted: Boolean, val texts: ReminderTexts) : ProfileIntent
    data object DisableReminders : ProfileIntent
    data class ChangeReminderTime(val hour: Int, val minute: Int, val texts: ReminderTexts) : ProfileIntent
}

sealed interface ProfileEffect {
    data class ShowMessage(val key: String) : ProfileEffect
    data object NavigatePaywall : ProfileEffect
    data object SignedOut : ProfileEffect
}
