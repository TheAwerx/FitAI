package com.fitai.app.notifications

import kotlinx.serialization.Serializable

/**
 * Everything the platform should have pending. Texts are already localized because platforms fire
 * (and Android re-arms after reboot) without Compose. Day numbers are ISO: 1 = Monday … 7 = Sunday.
 * Skip dates are ISO local dates ("2026-11-09") where an onboarding message replaces the repeating one.
 */
@Serializable
data class ReminderSchedule(
    val workoutReminder: WorkoutReminder? = null,
    val mealNudge: MealNudge? = null,
    val onboardingSequence: OnboardingSequence? = null,
    val trialEndReminder: TrialEndReminder? = null,
)

@Serializable
data class WorkoutReminder(
    val days: Set<Int>,
    val hour: Int,
    val minute: Int,
    val title: String,
    val body: String,
    val skipDates: Set<String> = emptySet(),
)

@Serializable
data class MealNudge(
    val hour: Int,
    val minute: Int,
    val title: String,
    val body: String,
    val skipDates: Set<String> = emptySet(),
)

/** Items fire on the local date of [anchorEpochMillis] plus [SequenceReminder.dayOffset]. */
@Serializable
data class OnboardingSequence(
    val anchorEpochMillis: Long,
    val items: List<SequenceReminder>,
)

@Serializable
data class SequenceReminder(
    val dayOffset: Int,
    val hour: Int,
    val minute: Int,
    val title: String,
    val body: String,
)

@Serializable
data class TrialEndReminder(
    val atEpochMillis: Long,
    val title: String,
    val body: String,
)

data class ReminderMessage(val title: String, val body: String)

/** Localized copy for every reminder, built from `Strings` in the UI layer. */
data class ReminderTexts(
    val workout: ReminderMessage,
    val meal: ReminderMessage,
    val day0FirstSession: ReminderMessage,
    val day1MealPhoto: ReminderMessage,
    val day2RestDay: ReminderMessage,
    val day3Overload: ReminderMessage,
    val day5TodayMode: ReminderMessage,
    val day7WeeklyReport: ReminderMessage,
    val trialEnd: ReminderMessage,
)

data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val permissionGranted: Boolean,
)
