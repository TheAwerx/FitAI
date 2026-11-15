package com.fitai.app.notifications

import com.fitai.app.access.AccessTier
import com.fitai.app.access.EntitlementRepository
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.trial.TrialRepository
import com.fitai.app.util.epochMillis
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Rebuilds and replaces every pending reminder; cancels them all when reminders are off. Also syncs the
 * push registration: while this device receives server pushes, they own the meal reminder and the
 * fixed local 12:30 one is left out, so the same nudge never arrives twice.
 */
class BuildAndScheduleRemindersUseCase(
    private val syncPushRegistrationUseCase: SyncPushRegistrationUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderStateRepository: ReminderStateRepository,
    private val reminderScheduler: ReminderScheduler,
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val entitlementRepository: EntitlementRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(texts: ReminderTexts): UseCaseState<Unit> = useCaseCatching {
        if (!userPreferencesRepository.notificationsEnabled) {
            reminderScheduler.cancelAll()
            syncPushRegistrationUseCase()
            return@useCaseCatching
        }
        val push = syncPushRegistrationUseCase()
        val serverOwnsMeals = (push as? UseCaseState.Success)?.data == PushRegistrationStatus.Registered
        val now = epochMillis()
        val anchor = reminderStateRepository.sequenceAnchorEpochMillis
            ?: now.also { reminderStateRepository.sequenceAnchorEpochMillis = it }
        val schedule = ReminderPlanner.plan(
            nowMillis = now,
            reminderHour = userPreferencesRepository.reminderHour,
            reminderMinute = userPreferencesRepository.reminderMinute,
            trainingDays = trainingDays(),
            sequenceAnchorMillis = anchor,
            trialExpiresAtMillis = trialExpiresAtMillis(),
            texts = texts,
            includeMealNudge = userPreferencesRepository.pushNotificationTypes.mealLogging && !serverOwnsMeals,
        )
        reminderScheduler.schedule(schedule)
    }

    private suspend fun trainingDays(): Set<Int> {
        val user = localUserRepository.getLatestUser()
        val plan = user?.let { workoutRepository.getActivePlan(it.id) }?.takeUnless { it.isAdHoc }
        val planDays = plan
            ?.let { workoutRepository.getExercisesForPlan(it.id) }
            ?.map { it.dayOfWeek }
            ?.filter { it in 1..7 }
            ?.toSet()
            .orEmpty()
        return planDays.ifEmpty { ReminderPlanner.defaultTrainingDays(userPreferencesRepository.commitmentDaysPerWeek) }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun trialExpiresAtMillis(): Long? {
        if (entitlementRepository.currentTier() != AccessTier.Trial) return null
        val status = trialRepository.getTrialStatus().getOrNull() ?: return null
        if (!status.isActive || status.hasSubscription) return null
        return runCatching { Instant.parse(status.trialExpiresAt).toEpochMilliseconds() }.getOrNull()
    }
}

/** Reminders off: drops local reminders and this device's push registration. */
class CancelRemindersUseCase(
    private val reminderScheduler: ReminderScheduler,
    private val pushDeviceRepository: PushDeviceRepository,
) {
    suspend operator fun invoke(): UseCaseState<Unit> = useCaseCatching {
        reminderScheduler.cancelAll()
        pushDeviceRepository.unregister()
    }
}

/** Only stores the flag; follow with [BuildAndScheduleRemindersUseCase] or [CancelRemindersUseCase]. */
class SetRemindersEnabledUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(enabled: Boolean): UseCaseState<Unit> = useCaseCatching {
        userPreferencesRepository.notificationsEnabled = enabled
    }
}

class UpdateReminderTimeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(hour: Int, minute: Int): UseCaseState<Unit> = useCaseCatching {
        require(hour in 0..23 && minute in 0..59) { "invalid_reminder_time" }
        userPreferencesRepository.reminderHour = hour
        userPreferencesRepository.reminderMinute = minute
    }
}

class GetReminderSettingsUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(): UseCaseState<ReminderSettings> = useCaseCatching {
        ReminderSettings(
            enabled = userPreferencesRepository.notificationsEnabled,
            hour = userPreferencesRepository.reminderHour,
            minute = userPreferencesRepository.reminderMinute,
            permissionGranted = reminderScheduler.isPermissionGranted(),
        )
    }
}
