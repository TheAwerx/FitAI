package com.fitai.app.engagement

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.PersonalRecords
import com.fitai.app.util.isoTimestampToLocalDate
import com.fitai.app.util.isoWeekKey
import com.fitai.app.util.nowIsoTimestamp
import com.fitai.app.util.todayLocalDate
import com.fitai.app.util.weekRangeIso
import com.fitai.app.util.weekStart
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val ALL_TIME_START_ISO = "1970-01-01T00:00:00Z"
private const val WEIGHT_TREND_DAYS = 90
private const val VOLUME_WEEKS = 8

private class EngagementHistory(
    val user: LocalUserProfile,
    val today: LocalDate,
    val trainingDaysOfWeek: Set<Int>,
    val facts: EngagementFacts,
) {
    fun streak(): Streaks.Result = Streaks.compute(today, facts.activeDates, trainingDaysOfWeek, Badges.streakMilestones)
}

private fun startOfDayIso(date: LocalDate): String = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()

private suspend fun loadEngagementHistory(
    localUserRepository: LocalUserRepository,
    workoutRepository: WorkoutRepository,
    nutritionRepository: NutritionRepository,
    progressRepository: ProgressRepository,
): EngagementHistory {
    val user = localUserRepository.getLatestUser() ?: error("no_user")
    val today = todayLocalDate()
    val end = startOfDayIso(today.plus(DatePeriod(days = 1)))
    val plan = workoutRepository.getActivePlan(user.id)?.takeUnless { it.isAdHoc }
    val trainingDays = plan?.let { workoutRepository.getExercisesForPlan(it.id).map { exercise -> exercise.dayOfWeek }.toSet() }
        .orEmpty()
    val facts = EngagementFacts.from(
        completedSessions = workoutRepository.getCompletedSessionsInRange(user.id, ALL_TIME_START_ISO, end),
        setHistory = workoutRepository.getCompletedSetHistory(user.id),
        meals = nutritionRepository.getMealsInRange(user.id, ALL_TIME_START_ISO, end),
        measurements = progressRepository.getMeasurementsForUser(user.id),
        diet = nutritionRepository.getActiveDiet(user.id),
        trainingDaysOfWeek = trainingDays,
    )
    return EngagementHistory(user, today, trainingDays, facts)
}

/** Records the current week's freeze in [UserPreferencesRepository.streakFreezeUsedWeek] once it is consumed. */
class GetEngagementSummaryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): UseCaseState<EngagementSummary> = useCaseCatching {
        val history = loadEngagementHistory(localUserRepository, workoutRepository, nutritionRepository, progressRepository)
        val streak = history.streak().status
        val currentWeek = isoWeekKey(history.today)
        if (streak.freezeUsedThisWeek && userPreferencesRepository.streakFreezeUsedWeek != currentWeek) {
            userPreferencesRepository.streakFreezeUsedWeek = currentWeek
        }
        EngagementSummary(
            streak = streak,
            weeklyXp = Xp.points(history.facts) { isoWeekKey(it) == currentWeek },
            level = Xp.levelFor(Xp.points(history.facts)),
            checklist = Day0.checklist(
                facts = history.facts,
                firstLaunch = isoTimestampToLocalDate(history.user.createdAt),
                today = history.today,
            ),
        )
    }
}

class GetWorkoutStreakUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(): UseCaseState<WorkoutStreak> = useCaseCatching {
        val history = loadEngagementHistory(localUserRepository, workoutRepository, nutritionRepository, progressRepository)
        val trainedDates = history.facts.sessionDates.toSet()
        val status = Streaks.compute(history.today, trainedDates, history.trainingDaysOfWeek).status
        val monday = weekStart(history.today)
        val week = (0 until 7).map { offset ->
            val date = monday.plus(DatePeriod(days = offset))
            StreakDay(
                date = date,
                trained = date in trainedDates,
                isToday = date == history.today,
                isTrainingDay = history.trainingDaysOfWeek.isEmpty() || (offset + 1) in history.trainingDaysOfWeek,
            )
        }
        WorkoutStreak(
            currentDays = status.currentDays,
            longestDays = status.longestDays,
            trainedToday = history.today in trainedDates,
            totalSessions = history.facts.sessionDates.size,
            week = week,
        )
    }
}

class GetStarterChallengeUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): UseCaseState<StarterChallenge> = useCaseCatching {
        val facts = loadEngagementHistory(localUserRepository, workoutRepository, nutritionRepository, progressRepository).facts
        val tasks = listOf(
            StarterTask(StarterTaskId.Notifications, userPreferencesRepository.notificationsEnabled),
            StarterTask(StarterTaskId.FirstWorkout, facts.sessionDates.isNotEmpty()),
            StarterTask(StarterTaskId.FirstMealPhoto, facts.photoMealDates.isNotEmpty()),
            StarterTask(StarterTaskId.FirstWeighIn, facts.weighInDates.isNotEmpty()),
        )
        StarterChallenge(
            tasks = tasks,
            visible = !userPreferencesRepository.starterChallengeDismissed && !userPreferencesRepository.starterGiftClaimed,
        )
    }
}

/** [giftClaimed] marks the challenge finished (gift opened); otherwise the user chose "don't show again". */
class HideStarterChallengeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(giftClaimed: Boolean): UseCaseState<Unit> = useCaseCatching {
        if (giftClaimed) {
            userPreferencesRepository.starterGiftClaimed = true
        } else {
            userPreferencesRepository.starterChallengeDismissed = true
        }
    }
}

class GetBadgesUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(): UseCaseState<List<Badge>> = useCaseCatching {
        val history = loadEngagementHistory(localUserRepository, workoutRepository, nutritionRepository, progressRepository)
        Badges.compute(history.facts, history.streak().milestoneDates)
    }
}

class AddWeighInUseCase(
    private val localUserRepository: LocalUserRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(weightKg: Double, waistCm: Double? = null): UseCaseState<BodyMeasurement> = useCaseCatching {
        require(weightKg in WEIGHT_RANGE_KG) { "invalid_weight" }
        require(waistCm == null || waistCm in WAIST_RANGE_CM) { "invalid_waist" }
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        progressRepository.addMeasurement(
            BodyMeasurement(
                id = "",
                userId = user.id,
                weightKg = weightKg,
                waistCm = waistCm,
                measuredAt = nowIsoTimestamp(),
            ),
        )
    }

    companion object {
        val WEIGHT_RANGE_KG = 20.0..400.0
        val WAIST_RANGE_CM = 30.0..300.0
    }
}

class GetProgressOverviewUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(): UseCaseState<ProgressOverview> = useCaseCatching {
        val history = loadEngagementHistory(localUserRepository, workoutRepository, nutritionRepository, progressRepository)
        val userId = history.user.id
        val trendStart = history.today.minus(DatePeriod(days = WEIGHT_TREND_DAYS - 1))
        val weightPoints = progressRepository
            .getMeasurementsInRange(userId, startOfDayIso(trendStart), startOfDayIso(history.today.plus(DatePeriod(days = 1))))
            .mapNotNull { m -> isoTimestampToLocalDate(m.measuredAt)?.let { it to m } }
            .sortedBy { it.second.measuredAt }
            .associate { (date, m) -> date to m.weightKg }
            .map { (date, weight) -> WeightPoint(date, weight) }

        val currentWeekStart = weekStart(history.today)
        val weeklyVolumes = (-(VOLUME_WEEKS - 1)..0).map { offset ->
            val (start, end) = weekRangeIso(offset)
            val monday = currentWeekStart.plus(DatePeriod(days = 7 * offset))
            WeeklyVolume(
                weekKey = isoWeekKey(monday),
                weekStart = monday,
                volumeKg = workoutRepository.getCompletedVolumeInRange(userId, start, end),
            )
        }

        ProgressOverview(
            weightPoints = weightPoints,
            latestWeightKg = progressRepository.getLatestMeasurement(userId)?.weightKg,
            weeklyVolumes = weeklyVolumes,
            personalRecords = PersonalRecords.bestPerExercise(workoutRepository.getCompletedSetHistory(userId)),
            badges = Badges.compute(history.facts, history.streak().milestoneDates),
        )
    }
}
