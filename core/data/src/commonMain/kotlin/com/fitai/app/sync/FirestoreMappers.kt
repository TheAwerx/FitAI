package com.fitai.app.sync

import com.fitai.app.db.Custom_foods
import com.fitai.app.model.ActivityLevel
import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.DietPlan
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.Exercise
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FocusArea
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.Gender
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.PlanFood
import com.fitai.app.model.PlanMeal
import com.fitai.app.model.PlanMealOption
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionLength
import com.fitai.app.model.SessionSet
import com.fitai.app.model.TrainingExperience
import com.fitai.app.model.TrainingObstacle
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.model.encodeProfileCodes
import com.fitai.app.preferences.AttributionSources
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A session with its exercises and sets, read from the device for upload. */
internal data class SessionSnapshot(
    val session: WorkoutSession,
    val exercises: List<SessionExerciseSnapshot>,
)

internal data class SessionExerciseSnapshot(
    val entry: SessionExercise,
    /** Null only if the plan exercise row is gone. */
    val exercise: Exercise?,
    val sets: List<SessionSet>,
)

/** Limits shared with `firestore.rules`; values are clamped here so a valid device row is never rejected. */
internal object CloudLimits {
    const val NAME = 80
    const val EMAIL = 254
    const val TIME_ZONE = 64
    const val TITLE = 200
    const val TEXT = 2000
    const val MEAL_NAME = 200
    const val PLAN_EXERCISES = 150
    const val SESSION_EXERCISES = 40
    val AGE = 10L..120L
    val HEIGHT_CM = 50.0..272.0
    val WEIGHT_KG = 20.0..500.0
    val DAYS_PER_WEEK = 1..7
    val SESSION_MINUTES = 5..240
    const val MAX_CALORIES = 10000
    const val MAX_MACRO_G = 2000
    const val MAX_BODY_FAT = 80.0
    const val MAX_CIRCUMFERENCE_CM = 300.0
    const val PLAN_MEALS = 8
    const val MEAL_OPTIONS = 3
    const val OPTION_FOODS = 12
    const val FOOD_NAME = 120
    const val MEAL_NOTE = 300
    const val FOOD_ID = 64
    const val MAX_FOOD_GRAMS = 5000.0
    const val MAX_KCAL_PER_100G = 900.0
    const val MAX_MACRO_PER_100G = 100.0
}

// users/{uid}

internal fun CloudUserProfile.toDocument(updatedAt: String): UserDocument = UserDocument(
    localUserId = profile.id,
    name = profile.name.trim().take(CloudLimits.NAME),
    email = profile.email.trim().take(CloudLimits.EMAIL),
    age = profile.age.coerceIn(CloudLimits.AGE),
    heightCm = profile.heightCm.coerceIn(CloudLimits.HEIGHT_CM),
    weightKg = profile.weightKg.coerceIn(CloudLimits.WEIGHT_KG),
    gender = Gender.fromCode(answers.gender)?.code,
    // Unknown codes fall back to what plan generation assumes for them.
    goal = FitnessGoal.fromCode(profile.goal)?.code ?: FitnessGoal.StayHealthy.code,
    activityLevel = ActivityLevel.fromCode(profile.activityLevel)?.code ?: ActivityLevel.Moderate.code,
    equipment = knownCodes(decodeProfileCodes(profile.equipment)) { EquipmentOption.fromCode(it) != null },
    dietRestrictions = knownCodes(decodeProfileCodes(profile.dietRestrictions)) { DietRestriction.fromCode(it) != null },
    experience = TrainingExperience.fromCode(answers.experience)?.code,
    obstacle = TrainingObstacle.fromCode(answers.obstacle)?.code,
    focusAreas = knownCodes(answers.focusAreas) { FocusArea.fromCode(it) != null }.take(FocusArea.MAX_SELECTED),
    daysPerWeek = answers.daysPerWeek.coerceIn(CloudLimits.DAYS_PER_WEEK),
    sessionMinutes = answers.sessionMinutes.coerceIn(CloudLimits.SESSION_MINUTES),
    attributionSource = settings.attributionSource?.takeIf { it in AttributionSources.all },
    commitmentConfirmedAt = settings.commitmentConfirmedAt?.takeIf(::isIsoInstant),
    notificationsEnabled = settings.notificationsEnabled,
    reminderHour = settings.reminderHour.coerceIn(0, 23),
    reminderMinute = settings.reminderMinute.coerceIn(0, 59),
    language = if (language == "en") "en" else "tr",
    timeZone = timeZone.take(CloudLimits.TIME_ZONE).ifBlank { "UTC" },
    createdAt = profile.createdAt.takeIf(::isIsoInstant) ?: updatedAt,
    updatedAt = updatedAt,
)

/** Same shape `firestore.rules` accepts (`isIso`): device UTC instants such as 2026-11-15T07:30:12.345Z. */
private val ISO_INSTANT = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:.]{5,24}Z$")

internal fun isIsoInstant(value: String): Boolean = ISO_INSTANT.matches(value)

/** True when the document holds a profile (functions could create a bare `users/{uid}` later). */
internal val UserDocument.hasProfile: Boolean get() = name.isNotBlank() || goal.isNotBlank()

internal fun UserDocument.toCloudProfile(): CloudUserProfile = CloudUserProfile(
    profile = LocalUserProfile(
        id = localUserId,
        name = name,
        email = email,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        activityLevel = activityLevel,
        equipment = encodeProfileCodes(equipment),
        dietRestrictions = encodeProfileCodes(dietRestrictions),
        firebaseUid = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    answers = OnboardingAnswers(
        gender = gender,
        experience = experience,
        obstacle = obstacle,
        focusAreas = focusAreas.toSet(),
        daysPerWeek = daysPerWeek,
        sessionMinutes = sessionMinutes,
    ),
    settings = CloudUserSettings(
        attributionSource = attributionSource,
        commitmentConfirmedAt = commitmentConfirmedAt,
        notificationsEnabled = notificationsEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
    ),
    language = language,
    timeZone = timeZone,
)

internal fun LegacyUserProfileDocument.toCloudProfile(): CloudUserProfile = CloudUserProfile(
    profile = LocalUserProfile(
        id = "",
        name = name,
        email = email,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        activityLevel = activityLevel,
        equipment = equipment,
        dietRestrictions = dietRestrictions,
        firebaseUid = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    answers = OnboardingAnswers(
        gender = null,
        experience = null,
        obstacle = null,
        focusAreas = emptySet(),
        daysPerWeek = UserDocument.DEFAULT_DAYS_PER_WEEK,
        sessionMinutes = SessionLength.DEFAULT,
    ),
    settings = CloudUserSettings(null, null, false, UserDocument.DEFAULT_REMINDER_HOUR, 0),
    language = "tr",
    timeZone = "UTC",
    legacy = true,
)

// workout_plans

internal fun WorkoutPlan.toDocument(exercises: List<Exercise>, updatedAt: String) = WorkoutPlanDocument(
    id = id,
    title = title.take(CloudLimits.TITLE),
    description = description.take(CloudLimits.TEXT),
    daysPerWeek = daysPerWeek.coerceIn(0, 7),
    kind = if (isAdHoc) WorkoutPlanDocument.KIND_ADHOC else WorkoutPlanDocument.KIND_PROGRAM,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    exercises = exercises.take(CloudLimits.PLAN_EXERCISES).map { it.toEntry() },
)

internal fun Exercise.toEntry() = ExerciseEntry(
    id = id,
    dayOfWeek = dayOfWeek,
    name = name,
    sets = sets,
    reps = reps,
    restSeconds = restSeconds,
    muscleGroup = muscleGroup,
    notes = notes,
)

internal fun ExerciseEntry.toExercise(planId: String) = Exercise(
    id = id,
    planId = planId,
    dayOfWeek = dayOfWeek,
    name = name,
    sets = sets,
    reps = reps,
    restSeconds = restSeconds,
    muscleGroup = muscleGroup,
    notes = notes,
)

// diet_plans

internal fun DietPlan.toDocument(updatedAt: String) = DietPlanDocument(
    id = id,
    dailyCalories = dailyCalories.coerceIn(0, CloudLimits.MAX_CALORIES),
    proteinG = proteinG.coerceIn(0, CloudLimits.MAX_MACRO_G),
    carbsG = carbsG.coerceIn(0, CloudLimits.MAX_MACRO_G),
    fatG = fatG.coerceIn(0, CloudLimits.MAX_MACRO_G),
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meals = meals.take(CloudLimits.PLAN_MEALS).map { meal ->
        DietMealEntry(
            slot = meal.slot,
            time = meal.time.take(5),
            selected = meal.selectedIndex.coerceAtMost(CloudLimits.MEAL_OPTIONS - 1),
            options = meal.options.take(CloudLimits.MEAL_OPTIONS).map { option ->
                DietMealOptionEntry(
                    title = option.title.take(CloudLimits.TITLE),
                    note = option.note.take(CloudLimits.MEAL_NOTE),
                    foods = option.foods.take(CloudLimits.OPTION_FOODS).map { it.toEntry() },
                )
            },
        )
    },
)

private fun PlanFood.toEntry() = DietFoodEntry(
    foodId = foodId?.takeIf(::isFoodId),
    name = name.take(CloudLimits.FOOD_NAME),
    amount = amount.coerceIn(0.0, CloudLimits.MAX_FOOD_GRAMS),
    unit = unit.takeIf { FoodUnits.isKnown(it) } ?: FoodUnits.GRAM,
    grams = grams.coerceIn(0.0, CloudLimits.MAX_FOOD_GRAMS),
    calories = calories.coerceIn(0, CloudLimits.MAX_CALORIES),
    proteinG = proteinG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
    carbsG = carbsG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
    fatG = fatG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
)

/** Restore: the detailed meals of a cloud plan. */
internal fun DietPlanDocument.toPlanMeals(): List<PlanMeal> = meals.map { meal ->
    PlanMeal(
        slot = meal.slot,
        time = meal.time,
        selected = meal.selected,
        options = meal.options.map { option ->
            PlanMealOption(
                title = option.title,
                note = option.note,
                foods = option.foods.map { food ->
                    PlanFood(
                        foodId = food.foodId,
                        name = food.name,
                        amount = food.amount,
                        unit = food.unit,
                        grams = food.grams,
                        calories = food.calories,
                        proteinG = food.proteinG,
                        carbsG = food.carbsG,
                        fatG = food.fatG,
                    )
                },
            )
        },
    )
}

private val FOOD_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")

internal fun isFoodId(value: String): Boolean = FOOD_ID_PATTERN.matches(value)

// workout_sessions

internal fun SessionSnapshot.toDocument(updatedAt: String, zone: TimeZone): WorkoutSessionDocument {
    val allSets = exercises.flatMap { it.sets }
    val doneSets = allSets.filter { it.completed }
    return WorkoutSessionDocument(
        id = session.id,
        planId = session.planId,
        startedAt = session.startedAt,
        completedAt = session.completedAt,
        completed = session.completedAt != null,
        durationMinutes = session.durationMinutes,
        notes = session.notes?.take(CloudLimits.TEXT),
        localDate = localDateOf(session.startedAt, zone),
        totalSets = allSets.size,
        completedSets = doneSets.size,
        volumeKg = doneSets.sumOf { (it.weightKg ?: 0.0) * leadingReps(it.actualReps ?: it.targetReps) },
        exercises = exercises.sortedBy { it.entry.orderIndex }.take(CloudLimits.SESSION_EXERCISES).map { item ->
            SessionExerciseEntry(
                sessionExerciseId = item.entry.id,
                exerciseId = item.entry.exerciseId,
                orderIndex = item.entry.orderIndex,
                name = item.exercise?.name.orEmpty(),
                muscleGroup = item.exercise?.muscleGroup.orEmpty(),
                sets = item.sets.sortedBy { it.setNumber }.map { set ->
                    SessionSetEntry(
                        id = set.id,
                        setNumber = set.setNumber,
                        targetReps = set.targetReps,
                        actualReps = set.actualReps,
                        weightKg = set.weightKg,
                        completed = set.completed,
                        completedAt = set.completedAt,
                    )
                },
            )
        },
        updatedAt = updatedAt,
    )
}

/** Same rule as the volume query: "8-12" counts as 8, "30 sn" as 30. */
internal fun leadingReps(reps: String): Int = reps.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0

// meal_logs

internal fun MealLog.toDocument(updatedAt: String, zone: TimeZone) = MealLogDocument(
    id = id,
    mealName = mealName.take(CloudLimits.MEAL_NAME),
    calories = calories.coerceIn(0, CloudLimits.MAX_CALORIES),
    proteinG = proteinG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
    carbsG = carbsG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
    fatG = fatG.coerceIn(0.0, CloudLimits.MAX_MACRO_G.toDouble()),
    source = if (hasPhoto) MealLogDocument.SOURCE_PHOTO else MealLogDocument.SOURCE_MANUAL,
    photoUrl = photoUrl,
    loggedAt = loggedAt,
    localDate = localDateOf(loggedAt, zone),
    updatedAt = updatedAt,
    slot = MealSlot.fromCode(slot)?.code,
    foodId = foodId?.takeIf(::isFoodId),
    servingUnit = servingUnit?.takeIf { FoodUnits.isKnown(it) },
    servingAmount = servingAmount?.coerceIn(0.0, CloudLimits.MAX_FOOD_GRAMS),
    grams = grams?.coerceIn(0.0, CloudLimits.MAX_FOOD_GRAMS),
)

internal fun MealLogDocument.toMealLog(userId: String) = MealLog(
    id = id,
    userId = userId,
    mealName = mealName,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    photoUrl = photoUrl,
    loggedAt = loggedAt,
    slot = slot,
    foodId = foodId,
    servingUnit = servingUnit,
    servingAmount = servingAmount,
    grams = grams,
)

// custom_foods

internal fun Custom_foods.toDocument(updatedAt: String) = CustomFoodDocument(
    id = id,
    name = name.take(CloudLimits.NAME),
    kcalPer100g = kcal_per_100g.coerceIn(0.0, CloudLimits.MAX_KCAL_PER_100G),
    proteinPer100g = protein_per_100g.coerceIn(0.0, CloudLimits.MAX_MACRO_PER_100G),
    carbsPer100g = carbs_per_100g.coerceIn(0.0, CloudLimits.MAX_MACRO_PER_100G),
    fatPer100g = fat_per_100g.coerceIn(0.0, CloudLimits.MAX_MACRO_PER_100G),
    servingUnit = serving_unit.takeIf { FoodUnits.isKnown(it) } ?: FoodUnits.GRAM,
    servingGrams = serving_grams.coerceIn(0.0, CloudLimits.MAX_FOOD_GRAMS),
    createdAt = created_at.takeIf(::isIsoInstant) ?: updatedAt,
    updatedAt = updatedAt,
)

internal fun CustomFoodDocument.toRow(userId: String) = Custom_foods(
    id = id,
    user_id = userId,
    name = name,
    kcal_per_100g = kcalPer100g,
    protein_per_100g = proteinPer100g,
    carbs_per_100g = carbsPer100g,
    fat_per_100g = fatPer100g,
    serving_unit = servingUnit.ifBlank { FoodUnits.GRAM },
    serving_grams = servingGrams.takeIf { it > 0 } ?: 100.0,
    created_at = createdAt,
    updated_at = updatedAt,
)

// measurements

internal fun BodyMeasurement.toDocument(updatedAt: String, zone: TimeZone) = MeasurementDocument(
    id = id,
    weightKg = weightKg.coerceIn(CloudLimits.WEIGHT_KG),
    bodyFatPct = bodyFatPct?.coerceIn(0.0, CloudLimits.MAX_BODY_FAT),
    chestCm = chestCm?.coerceIn(0.0, CloudLimits.MAX_CIRCUMFERENCE_CM),
    waistCm = waistCm?.coerceIn(0.0, CloudLimits.MAX_CIRCUMFERENCE_CM),
    hipCm = hipCm?.coerceIn(0.0, CloudLimits.MAX_CIRCUMFERENCE_CM),
    armCm = armCm?.coerceIn(0.0, CloudLimits.MAX_CIRCUMFERENCE_CM),
    photoUrl = photoUrl,
    measuredAt = measuredAt,
    localDate = localDateOf(measuredAt, zone),
    updatedAt = updatedAt,
)

internal fun MeasurementDocument.toMeasurement(userId: String) = BodyMeasurement(
    id = id,
    userId = userId,
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    chestCm = chestCm,
    waistCm = waistCm,
    hipCm = hipCm,
    armCm = armCm,
    photoUrl = photoUrl,
    measuredAt = measuredAt,
)

// helpers

/** `yyyy-MM-dd` of an ISO instant in [zone]; empty when it can't be parsed. */
@OptIn(ExperimentalTime::class)
internal fun localDateOf(iso: String, zone: TimeZone): String =
    runCatching { Instant.parse(iso).toLocalDateTime(zone).date.toString() }.getOrDefault("")

/**
 * The plan restore makes active: the newest (by `createdAt`) of the cloud plans marked active. Older
 * clients could leave more than one active, so only one wins.
 */
internal fun <T> newestActiveId(plans: List<T>, isActive: (T) -> Boolean, createdAt: (T) -> String, id: (T) -> String): String? =
    plans.filter(isActive).maxByOrNull(createdAt)?.let(id)?.takeIf { it.isNotBlank() }

private fun knownCodes(codes: Collection<String>, isKnown: (String) -> Boolean): List<String> =
    codes.map { it.trim() }.filter { it.isNotEmpty() && isKnown(it) }.distinct().sorted()
