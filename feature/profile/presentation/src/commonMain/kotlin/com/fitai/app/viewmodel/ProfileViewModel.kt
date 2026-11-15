package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.AccessTier
import com.fitai.app.access.GetAccessTierUseCase
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.engagement.GetWorkoutHistoryUseCase
import com.fitai.app.engagement.GetWorkoutStreakUseCase
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.auth.IsFirebaseAvailableUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.model.encodeProfileCodes
import com.fitai.app.model.toggleDietRestriction
import com.fitai.app.model.toggleEquipmentOption
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.CancelRemindersUseCase
import com.fitai.app.notifications.GetReminderSettingsUseCase
import com.fitai.app.notifications.ReminderTexts
import com.fitai.app.notifications.SetRemindersEnabledUseCase
import com.fitai.app.notifications.UpdateReminderTimeUseCase
import com.fitai.app.profile.ObserveLocalUserUseCase
import com.fitai.app.profile.ObserveProfileAuthUseCase
import com.fitai.app.profile.ProfileSignInWithAppleUseCase
import com.fitai.app.profile.ProfileSignInWithGoogleUseCase
import com.fitai.app.profile.ProfileSignOutUseCase
import com.fitai.app.profile.RegeneratePlansUseCase
import com.fitai.app.profile.UpdateProfileInput
import com.fitai.app.profile.UpdateProfileUseCase
import com.fitai.app.viewmodel.profile.ProfileEffect
import com.fitai.app.viewmodel.profile.ProfileIntent
import com.fitai.app.viewmodel.profile.ProfileState
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val isFirebaseAvailableUseCase: IsFirebaseAvailableUseCase,
    private val observeLocalUserUseCase: ObserveLocalUserUseCase,
    private val observeProfileAuthUseCase: ObserveProfileAuthUseCase,
    private val profileSignOutUseCase: ProfileSignOutUseCase,
    private val profileSignInWithGoogleUseCase: ProfileSignInWithGoogleUseCase,
    private val profileSignInWithAppleUseCase: ProfileSignInWithAppleUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val regeneratePlansUseCase: RegeneratePlansUseCase,
    private val getReminderSettingsUseCase: GetReminderSettingsUseCase,
    private val setRemindersEnabledUseCase: SetRemindersEnabledUseCase,
    private val updateReminderTimeUseCase: UpdateReminderTimeUseCase,
    private val buildAndScheduleRemindersUseCase: BuildAndScheduleRemindersUseCase,
    private val cancelRemindersUseCase: CancelRemindersUseCase,
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val getWorkoutStreakUseCase: GetWorkoutStreakUseCase,
    private val getAccessTierUseCase: GetAccessTierUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<ProfileState, ProfileIntent, ProfileEffect>(
    ProfileState(isFirebaseAvailable = isFirebaseAvailableUseCase()),
) {

    init {
        loadReminders()
        refreshStats()
        viewModelScope.launch {
            observeLocalUserUseCase().collect { user ->
                setState {
                    if (editing) {
                        copy(localUser = user)
                    } else {
                        copy(localUser = user).withEditFromUser(user)
                    }
                }
            }
        }
        viewModelScope.launch {
            observeProfileAuthUseCase().collect { user ->
                setState { copy(authUser = user) }
            }
        }
    }

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh -> refreshStats()
            is ProfileIntent.SelectTab -> setState { copy(tab = intent.tab) }
            is ProfileIntent.SelectTrendMetric -> setState { copy(trendMetric = intent.metric) }
            is ProfileIntent.SelectTrendWeeks -> setState { copy(trendWeeks = intent.weeks) }
            ProfileIntent.SignOut -> signOut()
            is ProfileIntent.GoogleIdToken -> onGoogleIdToken(intent.token)
            is ProfileIntent.AppleTokens -> onAppleTokens(intent.idToken, intent.rawNonce)
            is ProfileIntent.ProviderError -> {
                val key = intent.message?.takeIf { it.isNotBlank() } ?: "error_unknown"
                setState { copy(message = key) }
            }
            ProfileIntent.ConsumeMessage -> setState { copy(message = null) }
            ProfileIntent.StartEdit -> setState {
                copy(editing = true, message = null).withEditFromUser(localUser)
            }
            ProfileIntent.CancelEdit -> setState {
                copy(editing = false, message = null).withEditFromUser(localUser)
            }
            is ProfileIntent.EditName -> setState { copy(editName = intent.value) }
            is ProfileIntent.EditEmail -> setState { copy(editEmail = intent.value) }
            is ProfileIntent.EditAge -> setState { copy(editAge = intent.value.filter(Char::isDigit)) }
            is ProfileIntent.EditHeight ->
                setState { copy(editHeightCm = intent.value.filter { it.isDigit() || it == '.' }) }
            is ProfileIntent.EditWeight ->
                setState { copy(editWeightKg = intent.value.filter { it.isDigit() || it == '.' }) }
            is ProfileIntent.EditGoal -> setState { copy(editGoal = intent.code) }
            is ProfileIntent.EditActivity -> setState { copy(editActivityLevel = intent.code) }
            is ProfileIntent.ToggleEquipment ->
                setState { copy(editEquipment = toggleEquipmentOption(editEquipment, intent.code)) }
            is ProfileIntent.ToggleDiet ->
                setState {
                    copy(editDietRestrictions = toggleDietRestriction(editDietRestrictions, intent.code))
                }
            ProfileIntent.SaveProfile -> saveProfile()
            ProfileIntent.RegeneratePlans -> regeneratePlans()
            is ProfileIntent.RemindersPermissionResolved -> onRemindersPermission(intent.granted, intent.texts)
            ProfileIntent.DisableReminders -> disableReminders()
            is ProfileIntent.ChangeReminderTime -> changeReminderTime(intent.hour, intent.minute, intent.texts)
        }
    }

    private fun signOut() {
        if (currentState().signingOut) return
        viewModelScope.launch {
            setState { copy(signingOut = true, message = null) }
            when (val result = profileSignOutUseCase()) {
                is UseCaseState.Success -> setEffect(ProfileEffect.SignedOut)
                is UseCaseState.Error -> setState { copy(signingOut = false, message = result.message) }
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val history = (getWorkoutHistoryUseCase() as? UseCaseState.Success)?.data
            val streak = (getWorkoutStreakUseCase() as? UseCaseState.Success)?.data
            val tier = (getAccessTierUseCase() as? UseCaseState.Success)?.data
            setState { copy(history = history, streak = streak, isPro = tier == AccessTier.Pro) }
        }
    }

    private fun loadReminders() {
        viewModelScope.launch {
            val result = getReminderSettingsUseCase()
            if (result is UseCaseState.Success) setState { copy(reminders = result.data) }
        }
    }

    private fun onRemindersPermission(granted: Boolean, texts: ReminderTexts) {
        if (currentState().remindersBusy) return
        trackEventUseCase(
            AnalyticsEvents.NOTIFICATION_PERMISSION_RESULT,
            mapOf(PARAM_RESULT to if (granted) RESULT_GRANTED else RESULT_DENIED, PARAM_SOURCE to SOURCE_PROFILE),
        )
        if (!granted) {
            setState { copy(message = MESSAGE_PERMISSION_DENIED) }
            loadReminders()
            return
        }
        updateReminders {
            setRemindersEnabledUseCase(true)
            buildAndScheduleRemindersUseCase(texts)
        }
    }

    private fun disableReminders() {
        if (currentState().remindersBusy) return
        updateReminders {
            setRemindersEnabledUseCase(false)
            cancelRemindersUseCase()
        }
    }

    private fun changeReminderTime(hour: Int, minute: Int, texts: ReminderTexts) {
        if (currentState().remindersBusy) return
        updateReminders {
            when (val saved = updateReminderTimeUseCase(hour, minute)) {
                is UseCaseState.Success -> buildAndScheduleRemindersUseCase(texts)
                is UseCaseState.Error -> saved
            }
        }
    }

    private fun updateReminders(block: suspend () -> UseCaseState<Unit>) {
        viewModelScope.launch {
            setState { copy(remindersBusy = true) }
            val result = block()
            val settings = getReminderSettingsUseCase()
            setState {
                copy(
                    remindersBusy = false,
                    reminders = (settings as? UseCaseState.Success)?.data ?: reminders,
                    message = if (result is UseCaseState.Error) MESSAGE_REMINDERS_FAILED else message,
                )
            }
        }
    }

    private fun saveProfile() {
        val s = currentState()
        val user = s.localUser ?: return
        val age = s.editAge.toLongOrNull()
        val height = s.editHeightCm.toDoubleOrNull()
        val weight = s.editWeightKg.toDoubleOrNull()
        if (s.editName.isBlank() || age == null || height == null || weight == null) {
            setState { copy(message = "validation_numbers_required") }
            return
        }
        if (s.editGoal.isBlank() || s.editActivityLevel.isBlank() || s.editEquipment.isEmpty()) {
            setState { copy(message = "validation_fields_required") }
            return
        }
        viewModelScope.launch {
            setState { copy(saving = true, message = null) }
            when (
                val result = updateProfileUseCase(
                    UpdateProfileInput(
                        id = user.id,
                        name = s.editName,
                        email = s.editEmail,
                        age = age,
                        heightCm = height,
                        weightKg = weight,
                        goal = s.editGoal,
                        activityLevel = s.editActivityLevel,
                        equipment = encodeProfileCodes(s.editEquipment),
                        dietRestrictions = encodeProfileCodes(s.editDietRestrictions),
                    ),
                )
            ) {
                is UseCaseState.Success ->
                    setState { copy(saving = false, editing = false, message = "profile_saved") }
                is UseCaseState.Error ->
                    setState { copy(saving = false, message = result.message) }
            }
        }
    }

    private fun regeneratePlans() {
        viewModelScope.launch {
            setState { copy(regenerating = true, message = null) }
            when (val result = regeneratePlansUseCase()) {
                is UseCaseState.Success ->
                    setState { copy(regenerating = false, message = "plans_regenerated") }
                is UseCaseState.Error -> {
                    if (result.message == "trial_expired") {
                        setEffect(ProfileEffect.NavigatePaywall)
                    }
                    setState { copy(regenerating = false, message = result.message) }
                }
            }
        }
    }

    private fun onGoogleIdToken(idToken: String) {
        if (!currentState().isFirebaseAvailable) {
            setState { copy(message = "firebase_unavailable") }
            return
        }
        viewModelScope.launch {
            when (val result = profileSignInWithGoogleUseCase(idToken)) {
                is UseCaseState.Success -> Unit
                is UseCaseState.Error -> setState { copy(message = result.message) }
            }
        }
    }

    private fun onAppleTokens(idToken: String, rawNonce: String) {
        if (!currentState().isFirebaseAvailable) {
            setState { copy(message = "firebase_unavailable") }
            return
        }
        viewModelScope.launch {
            when (val result = profileSignInWithAppleUseCase(idToken, rawNonce)) {
                is UseCaseState.Success -> Unit
                is UseCaseState.Error -> setState { copy(message = result.message) }
            }
        }
    }
}

private const val PARAM_RESULT = "result"
private const val PARAM_SOURCE = "source"
private const val SOURCE_PROFILE = "profile"
private const val RESULT_GRANTED = "granted"
private const val RESULT_DENIED = "denied"
private const val MESSAGE_PERMISSION_DENIED = "reminders_permission_denied"
private const val MESSAGE_REMINDERS_FAILED = "reminders_update_failed"

private fun ProfileState.withEditFromUser(user: com.fitai.app.model.LocalUserProfile?): ProfileState {
    if (user == null) return this
    return copy(
        editName = user.name,
        editEmail = user.email,
        editAge = user.age.toString(),
        editHeightCm = user.heightCm.toString(),
        editWeightKg = user.weightKg.toString(),
        editGoal = user.goal,
        editActivityLevel = user.activityLevel,
        editEquipment = decodeProfileCodes(user.equipment),
        editDietRestrictions = decodeProfileCodes(user.dietRestrictions).ifEmpty {
            setOf(com.fitai.app.model.DietRestriction.None.code)
        },
    )
}
