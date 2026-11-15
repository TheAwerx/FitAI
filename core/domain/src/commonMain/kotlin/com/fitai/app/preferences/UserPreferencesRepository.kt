package com.fitai.app.preferences

import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.notifications.PushNotificationTypes
import com.fitai.app.model.SessionLength
import com.fitai.app.model.decodeProfileCodes

interface UserPreferencesRepository {
    /** One of [AttributionSources]. */
    var attributionSource: String?
    var commitmentDaysPerWeek: Int?
    var commitmentConfirmedAt: String?
    var reminderHour: Int
    var reminderMinute: Int
    var notificationsEnabled: Boolean
    /** Settings → Smart notifications; follows the account (`users/{uid}.notificationTypes`). */
    var pushNotificationTypes: PushNotificationTypes
    /** ISO week key, e.g. "2026-W45" (see `isoWeekKey`). */
    var streakFreezeUsedWeek: String?
    /** Codes from `Gender`, `TrainingExperience`, `TrainingObstacle`; [focusAreas] is comma-joined `FocusArea` codes. */
    var gender: String?
    var trainingExperience: String?
    var trainingObstacle: String?
    var focusAreas: String?
    var sessionMinutes: Int?
    var starterChallengeDismissed: Boolean
    var starterGiftClaimed: Boolean
}

fun UserPreferencesRepository.onboardingAnswers(defaultDaysPerWeek: Int = DEFAULT_DAYS_PER_WEEK) = OnboardingAnswers(
    gender = gender,
    experience = trainingExperience,
    obstacle = trainingObstacle,
    focusAreas = focusAreas?.let(::decodeProfileCodes).orEmpty(),
    daysPerWeek = commitmentDaysPerWeek ?: defaultDaysPerWeek,
    sessionMinutes = sessionMinutes ?: SessionLength.DEFAULT,
)

private const val DEFAULT_DAYS_PER_WEEK = 3

object AttributionSources {
    const val TIKTOK = "tiktok"
    const val INSTAGRAM = "instagram"
    const val YOUTUBE = "youtube"
    const val FRIEND = "friend"
    const val GYM = "gym"
    const val SEARCH = "search"
    const val OTHER = "other"

    val all = listOf(TIKTOK, INSTAGRAM, YOUTUBE, FRIEND, GYM, SEARCH, OTHER)
}
