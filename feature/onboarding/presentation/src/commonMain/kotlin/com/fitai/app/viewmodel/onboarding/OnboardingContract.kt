package com.fitai.app.viewmodel.onboarding

import com.fitai.app.model.DietRestriction
import com.fitai.app.model.SessionLength

enum class StepKind {
    /** Feature showcase before the questions: page dots, no back button or progress bar. */
    Intro,
    /** A question with the back button and progress bar. */
    Question,
    /** Agree / disagree card; answering moves on. */
    Statement,
    /** Full-bleed story slide with page dots. */
    Story,
}

/** Steps in display order; [analyticsName] is the `step` param of ONBOARDING_STEP_VIEWED. */
enum class OnboardingStep(val analyticsName: String, val kind: StepKind) {
    IntroTrack("intro_track", StepKind.Intro),
    IntroProgress("intro_progress", StepKind.Intro),
    IntroPlate("intro_plate", StepKind.Intro),
    Goal("goal", StepKind.Question),
    Obstacle("obstacle", StepKind.Question),
    Focus("focus", StepKind.Question),
    Attribution("attribution", StepKind.Question),
    Experience("experience", StepKind.Question),
    Gender("gender", StepKind.Question),
    Name("name", StepKind.Question),
    Age("age", StepKind.Question),
    Height("height", StepKind.Question),
    Weight("weight", StepKind.Question),
    StatementConsistency("statement_consistency", StepKind.Statement),
    StatementNutrition("statement_nutrition", StepKind.Statement),
    StoryData("story_data", StepKind.Story),
    StorySystem("story_system", StepKind.Story),
    StoryPlan("story_plan", StepKind.Story),
    Activity("activity", StepKind.Question),
    Equipment("equipment", StepKind.Question),
    Days("days", StepKind.Question),
    Duration("duration", StepKind.Question),
    Diet("diet", StepKind.Question),
    Notifications("notifications", StepKind.Question),
    ;

    /** Position inside its run of slides (intro or story) for the page dots; both runs have [PAGE_COUNT]. */
    val pageIndex: Int
        get() = when (this) {
            IntroProgress, StorySystem -> 1
            IntroPlate, StoryPlan -> 2
            else -> 0
        }

    companion object {
        const val PAGE_COUNT = 3
    }
}

enum class OnboardingPhase(val analyticsName: String) {
    Profile("profile"),
    Analyzing("analyzing"),
    SaveError("save_error"),
    Result("result"),
    Quote("quote"),
    Pledge("pledge"),
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.entries.first(),
    val phase: OnboardingPhase = OnboardingPhase.Profile,
    val name: String = "",
    val age: String = "25",
    val heightCm: String = "175",
    val weightKg: String = "75.5",
    val goal: String = "",
    val obstacle: String? = null,
    val focusAreas: Set<String> = emptySet(),
    val experience: String? = null,
    val gender: String? = null,
    val activityLevel: String = "",
    val equipment: Set<String> = emptySet(),
    val daysPerWeek: Int? = null,
    val sessionMinutes: Int? = null,
    val dietRestrictions: Set<String> = setOf(DietRestriction.None.code),
    val attributionSource: String? = null,
    val focusLimitReached: Boolean = false,
    /** 0..100 while [OnboardingPhase.Analyzing] runs. */
    val analyzingPercent: Int = 0,
    val goalDate: GoalDate? = null,
    val saving: Boolean = false,
    val pledgeSigned: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Share of question steps behind the user; intros and stories don't move the bar. */
    val progress: Float
        get() {
            val tracked = OnboardingStep.entries.filter { it.kind != StepKind.Intro }
            val index = tracked.indexOf(step).coerceAtLeast(0)
            return (index + 1).toFloat() / (tracked.size + 1)
        }

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.Goal -> goal.isNotBlank()
            OnboardingStep.Obstacle -> obstacle != null
            OnboardingStep.Focus -> focusAreas.isNotEmpty()
            OnboardingStep.Experience -> experience != null
            OnboardingStep.Gender -> gender != null
            OnboardingStep.Name -> name.isNotBlank()
            OnboardingStep.Activity -> activityLevel.isNotBlank()
            OnboardingStep.Equipment -> equipment.isNotEmpty()
            OnboardingStep.Days -> daysPerWeek != null
            OnboardingStep.Duration -> sessionMinutes != null
            OnboardingStep.Diet -> dietRestrictions.isNotEmpty()
            else -> true
        } && !saving

    val resolvedDays: Int get() = daysPerWeek ?: DEFAULT_DAYS
    val resolvedMinutes: Int get() = sessionMinutes ?: SessionLength.DEFAULT

    companion object {
        const val DEFAULT_DAYS = 3
        val DAY_OPTIONS = 2..6
    }
}

data class GoalDate(val day: Int, val month: Int, val year: Int)

sealed interface OnboardingIntent {
    data class NameChanged(val value: String) : OnboardingIntent
    data class AgeChanged(val value: String) : OnboardingIntent
    data class HeightChanged(val value: String) : OnboardingIntent
    data class WeightChanged(val value: String) : OnboardingIntent
    data class GoalSelected(val code: String) : OnboardingIntent
    data class ObstacleSelected(val code: String) : OnboardingIntent
    data class FocusToggled(val code: String) : OnboardingIntent
    data class ExperienceSelected(val code: String) : OnboardingIntent
    data class GenderSelected(val code: String) : OnboardingIntent
    data class ActivitySelected(val code: String) : OnboardingIntent
    data class EquipmentToggled(val code: String) : OnboardingIntent
    data class DaysSelected(val days: Int) : OnboardingIntent
    data class DurationSelected(val minutes: Int) : OnboardingIntent
    data class DietToggled(val code: String) : OnboardingIntent
    data class AttributionSelected(val source: String) : OnboardingIntent
    data class StatementAnswered(val agree: Boolean) : OnboardingIntent
    data class NotificationsResolved(val granted: Boolean) : OnboardingIntent
    data object NextStep : OnboardingIntent
    data object PreviousStep : OnboardingIntent
    data object RetrySave : OnboardingIntent
    data object ContinueFromResult : OnboardingIntent
    data object ContinueFromQuote : OnboardingIntent
    data object ConfirmPledge : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object NavigateToPaywall : OnboardingEffect
    data object NavigateBackToWelcome : OnboardingEffect
}
