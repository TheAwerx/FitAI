package com.fitai.app.data

import com.fitai.app.session.SessionPreferenceKeys
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/** Where an unfinished first-run funnel resumes after the app is killed. */
enum class FunnelStage { Paywall, Signup, PlanSetup }

class AppPreferences(
    private val settings: Settings,
) {
    var onboardingCompleted: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = settings.set(KEY_ONBOARDING_COMPLETED, value)

    var welcomeSeen: Boolean
        get() = settings.getBoolean(KEY_WELCOME_SEEN, false)
        set(value) = settings.set(KEY_WELCOME_SEEN, value)

    var funnelStage: FunnelStage?
        get() = settings.getStringOrNull(KEY_FUNNEL_STAGE)?.let { name -> FunnelStage.entries.find { it.name == name } }
        set(value) = if (value == null) settings.remove(KEY_FUNNEL_STAGE) else settings.set(KEY_FUNNEL_STAGE, value.name)

    /** "tr" / "en" picked on Welcome; null follows the device language. Kept across sign-out. */
    var languageOverride: String?
        get() = settings.getStringOrNull(SessionPreferenceKeys.LANGUAGE)
        set(value) = if (value == null) settings.remove(SessionPreferenceKeys.LANGUAGE) else settings.set(SessionPreferenceKeys.LANGUAGE, value)

    /** Set by sign-out so the next launch opens login instead of the previous account's home. */
    var requiresLogin: Boolean
        get() = settings.getBoolean(SessionPreferenceKeys.REQUIRES_LOGIN, false)
        set(value) = if (value) settings.set(SessionPreferenceKeys.REQUIRES_LOGIN, true) else settings.remove(SessionPreferenceKeys.REQUIRES_LOGIN)

    private companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_FUNNEL_STAGE = "funnel_stage"
    }
}
