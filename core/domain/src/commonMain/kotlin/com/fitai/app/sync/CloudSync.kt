package com.fitai.app.sync

import com.fitai.app.model.LocalUserProfile
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.encodeProfileCodes
import com.fitai.app.notifications.PushNotificationTypes
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.preferences.onboardingAnswers
import com.fitai.app.util.appLocale
import kotlinx.datetime.TimeZone

/**
 * The account document `users/{uid}` (see `firebase/SCHEMA.md`): profile, every onboarding answer,
 * and the settings that follow the account to a new device.
 */
data class CloudUserProfile(
    val profile: LocalUserProfile,
    val answers: OnboardingAnswers,
    val settings: CloudUserSettings,
    /** "tr" / "en" when written; lets server-sent text match the app. */
    val language: String,
    /** IANA zone of the device when written, for server-side "today". */
    val timeZone: String,
    /** Read from the pre-schema `user_profiles/{uid}` document, which has no answers or settings. */
    val legacy: Boolean = false,
)

data class CloudUserSettings(
    val attributionSource: String?,
    val commitmentConfirmedAt: String?,
    /** Informational for the server; a new device asks for notification permission again. */
    val notificationsEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    /** Smart notification switches; restored on a new device like the reminder time. */
    val pushTypes: PushNotificationTypes = PushNotificationTypes(),
)

/** Rows copied from the cloud into the device by [UserDataSyncRepository.restoreAll]. */
data class CloudRestoreSummary(
    val workoutPlans: Int = 0,
    val dietPlans: Int = 0,
    val sessions: Int = 0,
    val meals: Int = 0,
    val measurements: Int = 0,
    val customFoods: Int = 0,
    val activeWorkoutPlanRestored: Boolean = false,
    val activeDietPlanRestored: Boolean = false,
) {
    val hasActivePlans: Boolean get() = activeWorkoutPlanRestored && activeDietPlanRestored
}

/** Cloud copy of plans and history under `users/{uid}/…`. The device database stays the source of truth. */
interface UserDataSyncRepository {
    val isFirebaseAvailable: Boolean

    /** Uploads every workout and diet plan, session, meal and measurement of [localUserId]. */
    suspend fun pushAll(uid: String, localUserId: String): Result<Unit>

    /**
     * Copies the account's cloud plans and history into the device for [localUserId]; rows already on the
     * device are skipped. With [keepLocalPlans] every restored plan is stored inactive so the device's own
     * plans stay active; otherwise the newest active cloud plan becomes active.
     */
    suspend fun restoreAll(uid: String, localUserId: String, keepLocalPlans: Boolean): Result<CloudRestoreSummary>
}

fun UserPreferencesRepository.cloudSettings() = CloudUserSettings(
    attributionSource = attributionSource,
    commitmentConfirmedAt = commitmentConfirmedAt,
    notificationsEnabled = notificationsEnabled,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    pushTypes = pushNotificationTypes,
)

/**
 * Builds the account document from the device copy. Onboarding doesn't ask for an email, so a blank local
 * email is filled from the signed-in [accountEmail].
 */
fun buildCloudUserProfile(
    local: LocalUserProfile,
    preferences: UserPreferencesRepository,
    accountEmail: String?,
    language: String = appLocale(),
    timeZone: String = TimeZone.currentSystemDefault().id,
) = CloudUserProfile(
    profile = if (local.email.isBlank() && !accountEmail.isNullOrBlank()) local.copy(email = accountEmail) else local,
    answers = preferences.onboardingAnswers(),
    settings = preferences.cloudSettings(),
    language = language,
    timeZone = timeZone,
)

/**
 * Writes restored onboarding answers and settings into device preferences. Notification permission is
 * per device, so `notificationsEnabled` is not restored. Legacy documents carry no answers and are skipped.
 */
fun UserPreferencesRepository.applyCloudProfile(cloud: CloudUserProfile) {
    if (cloud.legacy) return
    val answers = cloud.answers
    gender = answers.gender
    trainingExperience = answers.experience
    trainingObstacle = answers.obstacle
    focusAreas = encodeProfileCodes(answers.focusAreas).ifBlank { null }
    commitmentDaysPerWeek = answers.daysPerWeek
    sessionMinutes = answers.sessionMinutes
    attributionSource = cloud.settings.attributionSource
    commitmentConfirmedAt = cloud.settings.commitmentConfirmedAt
    reminderHour = cloud.settings.reminderHour
    reminderMinute = cloud.settings.reminderMinute
    pushNotificationTypes = cloud.settings.pushTypes
}
