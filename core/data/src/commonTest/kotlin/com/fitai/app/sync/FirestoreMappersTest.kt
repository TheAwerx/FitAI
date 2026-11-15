package com.fitai.app.sync

import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Exercise
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.model.MealLog
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import kotlinx.datetime.TimeZone
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreMappersTest {

    private val istanbul = TimeZone.of("Europe/Istanbul")
    private val now = "2026-11-15T09:00:00Z"

    private val cloudProfile = CloudUserProfile(
        profile = LocalUserProfile(
            id = "local-1",
            name = "Ada",
            email = "ada@example.com",
            age = 31,
            heightCm = 168.0,
            weightKg = 64.5,
            goal = "build_muscle",
            activityLevel = "moderate",
            equipment = "pull_up_bar,dumbbells",
            dietRestrictions = "vegetarian,gluten-free",
            firebaseUid = "uid-1",
            createdAt = "2026-11-01T10:00:00Z",
            updatedAt = "2026-11-02T10:00:00Z",
        ),
        answers = OnboardingAnswers(
            gender = "female",
            experience = "beginner",
            obstacle = "time",
            focusAreas = setOf("smart_overload", "plate_photo"),
            daysPerWeek = 4,
            sessionMinutes = 45,
        ),
        settings = CloudUserSettings("instagram", "2026-11-01T10:05:00Z", true, 7, 15),
        language = "en",
        timeZone = "Europe/Istanbul",
    )

    @Test
    fun userDocumentHoldsEveryOnboardingFieldAsCodes() {
        val doc = cloudProfile.toDocument(now)
        assertEquals(CLOUD_SCHEMA_VERSION, doc.schemaVersion)
        assertEquals("local-1", doc.localUserId)
        assertEquals(listOf("dumbbells", "pull_up_bar"), doc.equipment)
        assertEquals(listOf("gluten-free", "vegetarian"), doc.dietRestrictions)
        assertEquals(listOf("plate_photo", "smart_overload"), doc.focusAreas)
        assertEquals("female", doc.gender)
        assertEquals("beginner", doc.experience)
        assertEquals("time", doc.obstacle)
        assertEquals(4, doc.daysPerWeek)
        assertEquals(45, doc.sessionMinutes)
        assertEquals("instagram", doc.attributionSource)
        assertEquals(7, doc.reminderHour)
        assertEquals("en", doc.language)
        assertEquals("Europe/Istanbul", doc.timeZone)
        assertEquals("2026-11-01T10:00:00Z", doc.createdAt)
        assertEquals(now, doc.updatedAt)
    }

    @Test
    fun userDocumentRoundTripsToTheSameProfileAndAnswers() {
        val back = cloudProfile.toDocument(now).toCloudProfile()
        assertEquals(cloudProfile.answers, back.answers)
        assertEquals(cloudProfile.settings, back.settings)
        assertEquals("dumbbells,pull_up_bar", back.profile.equipment)
        assertEquals("gluten-free,vegetarian", back.profile.dietRestrictions)
        assertEquals(cloudProfile.profile.copy(equipment = back.profile.equipment, dietRestrictions = back.profile.dietRestrictions, firebaseUid = null, updatedAt = now), back.profile)
        assertFalse(back.legacy)
    }

    @Test
    fun unknownCodesAndOutOfRangeValuesAreSanitizedForTheRules() {
        val messy = cloudProfile.copy(
            profile = cloudProfile.profile.copy(goal = "get_huge", activityLevel = "", equipment = "dumbbells,laser", age = 7, weightKg = 900.0, name = "x".repeat(200)),
            answers = cloudProfile.answers.copy(gender = "other", obstacle = "weather", focusAreas = setOf("streak", "ai_coach", "log_lifts", "plate_photo", "nope"), daysPerWeek = 12),
            settings = cloudProfile.settings.copy(attributionSource = "tv", reminderHour = 30),
            language = "de",
        )
        val doc = messy.toDocument(now)
        assertEquals("stay_healthy", doc.goal)
        assertEquals("moderate", doc.activityLevel)
        assertEquals(listOf("dumbbells"), doc.equipment)
        assertEquals(10, doc.age)
        assertEquals(500.0, doc.weightKg)
        assertEquals(80, doc.name.length)
        assertNull(doc.gender)
        assertNull(doc.obstacle)
        assertEquals(3, doc.focusAreas.size)
        assertEquals(7, doc.daysPerWeek)
        assertNull(doc.attributionSource)
        assertEquals(23, doc.reminderHour)
        assertEquals("tr", doc.language)

        val oddDates = cloudProfile.copy(
            profile = cloudProfile.profile.copy(createdAt = "15.11.2026"),
            settings = cloudProfile.settings.copy(commitmentConfirmedAt = "yesterday"),
        ).toDocument(now)
        assertEquals(now, oddDates.createdAt)
        assertNull(oddDates.commitmentConfirmedAt)
        assertTrue(isIsoInstant("2026-11-15T07:30:12Z"))
        assertTrue(isIsoInstant("2026-11-15T07:30:12.123456789Z"))
    }

    @Test
    fun legacyProfileIsMarkedAndKeepsCommaCodes() {
        val legacy = LegacyUserProfileDocument(name = "Ali", age = 40, goal = "lose_fat", equipment = "bodyweight").toCloudProfile()
        assertTrue(legacy.legacy)
        assertEquals("", legacy.profile.id)
        assertEquals("bodyweight", legacy.profile.equipment)
        assertTrue(UserDocument().hasProfile.not())
    }

    @Test
    fun workoutPlanKeepsArchivedExercisesAndKind() {
        val plan = WorkoutPlan("p1", "u", "Push", "desc", 0, "2026-11-10T08:00:00Z", isActive = false)
        val exercises = listOf(
            Exercise("e1", "p1", 1, "Bench press", 4, "8-12", 90, "Chest", null),
            Exercise("e2", "p1", -1, "Old row", 3, "10", 60, "Back", "replaced"),
        )
        val doc = plan.toDocument(exercises, now)
        assertEquals(WorkoutPlanDocument.KIND_ADHOC, doc.kind)
        assertEquals(2, doc.exercises.size)
        assertEquals(-1, doc.exercises[1].dayOfWeek)
        assertEquals(exercises[1], doc.exercises[1].toExercise("p1"))
    }

    @Test
    fun sessionDocumentHasLocalDateTotalsAndVolume() {
        val session = WorkoutSession("s1", "p1", "2026-11-14T22:30:00Z", "2026-11-14T23:20:00Z", 50, null)
        val snapshot = SessionSnapshot(
            session = session,
            exercises = listOf(
                SessionExerciseSnapshot(
                    entry = SessionExercise("se2", "s1", "e2", 1),
                    exercise = Exercise("e2", "p1", 1, "Row", 3, "8-12", 90, "Back", null),
                    sets = listOf(SessionSet("x3", "se2", 1, "8-12", null, 40.0, true, "2026-11-14T22:50:00Z")),
                ),
                SessionExerciseSnapshot(
                    entry = SessionExercise("se1", "s1", "e1", 0),
                    exercise = Exercise("e1", "p1", 1, "Squat", 3, "5", 120, "Quads", null),
                    sets = listOf(
                        SessionSet("x2", "se1", 2, "5", null, null, false, null),
                        SessionSet("x1", "se1", 1, "5", "6", 100.0, true, "2026-11-14T22:40:00Z"),
                    ),
                ),
            ),
        )
        val doc = snapshot.toDocument(now, istanbul)
        // 22:30 UTC is already the next day in Istanbul (UTC+3).
        assertEquals("2026-11-15", doc.localDate)
        assertTrue(doc.completed)
        assertEquals(3, doc.totalSets)
        assertEquals(2, doc.completedSets)
        assertEquals(100.0 * 6 + 40.0 * 8, doc.volumeKg)
        assertEquals(listOf("se1", "se2"), doc.exercises.map { it.sessionExerciseId })
        assertEquals(listOf(1, 2), doc.exercises[0].sets.map { it.setNumber })
        assertEquals("Squat", doc.exercises[0].name)
    }

    @Test
    fun mealAndMeasurementRoundTrip() {
        val meal = MealLog("m1", "u", "Mercimek", 420, 22.5, 50.0, 12.0, MealLog.ANALYZED_PHOTO_MARKER, "2026-11-15T11:00:00Z")
        val mealDoc = meal.toDocument(now, istanbul)
        assertEquals(MealLogDocument.SOURCE_PHOTO, mealDoc.source)
        assertEquals("2026-11-15", mealDoc.localDate)
        assertEquals(meal.copy(userId = "u2"), mealDoc.toMealLog("u2"))

        val measurement = BodyMeasurement("w1", "u", 72.4, waistCm = 81.0, measuredAt = "2026-11-15T06:00:00Z")
        val mDoc = measurement.toDocument(now, istanbul)
        assertEquals("2026-11-15", mDoc.localDate)
        assertEquals(measurement, mDoc.toMeasurement("u"))
    }

    @Test
    fun dietPlanDocumentClampsMacros() {
        val doc = DietPlan("d1", "u", 99999, 150, -5, 70, "2026-11-10T08:00:00Z", true).toDocument(now)
        assertEquals(10000, doc.dailyCalories)
        assertEquals(0, doc.carbsG)
        assertTrue(doc.isActive)
    }

    @Test
    fun restoreActivatesOnlyTheNewestActivePlan() {
        val plans = listOf(
            WorkoutPlanDocument(id = "old", isActive = true, createdAt = "2026-10-01T00:00:00Z"),
            WorkoutPlanDocument(id = "new", isActive = true, createdAt = "2026-11-01T00:00:00Z"),
            WorkoutPlanDocument(id = "off", isActive = false, createdAt = "2026-12-01T00:00:00Z"),
        )
        assertEquals("new", newestActiveId(plans, { it.isActive }, { it.createdAt }, { it.id }))
        assertNull(newestActiveId(plans.filter { !it.isActive }, { it.isActive }, { it.createdAt }, { it.id }))
    }

    /** Must match the `hasExactly` key lists in firebase/firestore.rules (and rules-tests/test/fixtures.mjs). */
    @Test
    fun documentKeysMatchFirestoreRules() {
        fun keys(descriptor: SerialDescriptor) = descriptor.elementNames.toSet()
        assertEquals(
            setOf(
                "schemaVersion", "localUserId", "name", "email", "age", "heightCm", "weightKg", "gender",
                "goal", "activityLevel", "equipment", "dietRestrictions", "experience", "obstacle",
                "focusAreas", "daysPerWeek", "sessionMinutes", "attributionSource", "commitmentConfirmedAt",
                "notificationsEnabled", "reminderHour", "reminderMinute", "language", "timeZone",
                "createdAt", "updatedAt",
            ),
            keys(UserDocument.serializer().descriptor),
        )
        assertEquals(
            setOf("schemaVersion", "id", "title", "description", "daysPerWeek", "kind", "isActive", "createdAt", "updatedAt", "exercises"),
            keys(WorkoutPlanDocument.serializer().descriptor),
        )
        assertEquals(
            setOf("schemaVersion", "id", "dailyCalories", "proteinG", "carbsG", "fatG", "isActive", "createdAt", "updatedAt"),
            keys(DietPlanDocument.serializer().descriptor),
        )
        assertEquals(
            setOf(
                "schemaVersion", "id", "planId", "startedAt", "completedAt", "completed", "durationMinutes",
                "notes", "localDate", "totalSets", "completedSets", "volumeKg", "exercises", "updatedAt",
            ),
            keys(WorkoutSessionDocument.serializer().descriptor),
        )
        assertEquals(
            setOf(
                "schemaVersion", "id", "mealName", "calories", "proteinG", "carbsG", "fatG", "source",
                "photoUrl", "loggedAt", "localDate", "updatedAt",
            ),
            keys(MealLogDocument.serializer().descriptor),
        )
        assertEquals(
            setOf(
                "schemaVersion", "id", "weightKg", "bodyFatPct", "chestCm", "waistCm", "hipCm", "armCm",
                "photoUrl", "measuredAt", "localDate", "updatedAt",
            ),
            keys(MeasurementDocument.serializer().descriptor),
        )
    }

    @Test
    fun repsAndDatesParseDefensively() {
        assertEquals(8, leadingReps("8-12"))
        assertEquals(30, leadingReps("30 sn"))
        assertEquals(0, leadingReps("AMRAP"))
        assertEquals("", localDateOf("not a date", istanbul))
    }
}
