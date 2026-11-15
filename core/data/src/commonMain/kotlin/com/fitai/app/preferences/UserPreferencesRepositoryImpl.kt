package com.fitai.app.preferences

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class UserPreferencesRepositoryImpl(
    private val settings: Settings,
) : UserPreferencesRepository {

    override var attributionSource: String?
        get() = settings.getStringOrNull(KEY_ATTRIBUTION_SOURCE)
        set(value) = putOrRemove(KEY_ATTRIBUTION_SOURCE, value)

    override var commitmentDaysPerWeek: Int?
        get() = settings.getIntOrNull(KEY_COMMITMENT_DAYS)
        set(value) = if (value == null) settings.remove(KEY_COMMITMENT_DAYS) else settings[KEY_COMMITMENT_DAYS] = value

    override var commitmentConfirmedAt: String?
        get() = settings.getStringOrNull(KEY_COMMITMENT_CONFIRMED_AT)
        set(value) = putOrRemove(KEY_COMMITMENT_CONFIRMED_AT, value)

    override var reminderHour: Int
        get() = settings.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) = settings.set(KEY_REMINDER_HOUR, value)

    override var reminderMinute: Int
        get() = settings.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = settings.set(KEY_REMINDER_MINUTE, value)

    override var notificationsEnabled: Boolean
        get() = settings.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = settings.set(KEY_NOTIFICATIONS_ENABLED, value)

    override var streakFreezeUsedWeek: String?
        get() = settings.getStringOrNull(KEY_STREAK_FREEZE_WEEK)
        set(value) = putOrRemove(KEY_STREAK_FREEZE_WEEK, value)

    override var gender: String?
        get() = settings.getStringOrNull(KEY_GENDER)
        set(value) = putOrRemove(KEY_GENDER, value)

    override var trainingExperience: String?
        get() = settings.getStringOrNull(KEY_EXPERIENCE)
        set(value) = putOrRemove(KEY_EXPERIENCE, value)

    override var trainingObstacle: String?
        get() = settings.getStringOrNull(KEY_OBSTACLE)
        set(value) = putOrRemove(KEY_OBSTACLE, value)

    override var focusAreas: String?
        get() = settings.getStringOrNull(KEY_FOCUS_AREAS)
        set(value) = putOrRemove(KEY_FOCUS_AREAS, value)

    override var sessionMinutes: Int?
        get() = settings.getIntOrNull(KEY_SESSION_MINUTES)
        set(value) = if (value == null) settings.remove(KEY_SESSION_MINUTES) else settings[KEY_SESSION_MINUTES] = value

    override var starterChallengeDismissed: Boolean
        get() = settings.getBoolean(KEY_CHALLENGE_DISMISSED, false)
        set(value) = settings.set(KEY_CHALLENGE_DISMISSED, value)

    override var starterGiftClaimed: Boolean
        get() = settings.getBoolean(KEY_GIFT_CLAIMED, false)
        set(value) = settings.set(KEY_GIFT_CLAIMED, value)

    private fun putOrRemove(key: String, value: String?) {
        if (value == null) settings.remove(key) else settings[key] = value
    }

    private companion object {
        const val DEFAULT_REMINDER_HOUR = 18
        const val KEY_ATTRIBUTION_SOURCE = "pref_attribution_source"
        const val KEY_COMMITMENT_DAYS = "pref_commitment_days_per_week"
        const val KEY_COMMITMENT_CONFIRMED_AT = "pref_commitment_confirmed_at"
        const val KEY_REMINDER_HOUR = "pref_reminder_hour"
        const val KEY_REMINDER_MINUTE = "pref_reminder_minute"
        const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
        const val KEY_STREAK_FREEZE_WEEK = "pref_streak_freeze_used_week"
        const val KEY_GENDER = "pref_gender"
        const val KEY_EXPERIENCE = "pref_training_experience"
        const val KEY_OBSTACLE = "pref_training_obstacle"
        const val KEY_FOCUS_AREAS = "pref_focus_areas"
        const val KEY_SESSION_MINUTES = "pref_session_minutes"
        const val KEY_CHALLENGE_DISMISSED = "pref_starter_challenge_dismissed"
        const val KEY_GIFT_CLAIMED = "pref_starter_gift_claimed"
    }
}
