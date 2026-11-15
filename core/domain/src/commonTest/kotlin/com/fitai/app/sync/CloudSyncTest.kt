package com.fitai.app.sync

import com.fitai.app.model.LocalUserProfile
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.toProfilePayload
import com.fitai.app.preferences.UserPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CloudSyncTest {

    private val local = LocalUserProfile(
        id = "local-1",
        name = "Ada",
        email = "",
        age = 30,
        heightCm = 170.0,
        weightKg = 70.0,
        goal = "lose_fat",
        activityLevel = "light",
        equipment = "dumbbells,pull_up_bar",
        dietRestrictions = "vegan",
        firebaseUid = "uid-1",
        createdAt = "2026-11-01T10:00:00Z",
        updatedAt = "2026-11-01T10:00:00Z",
    )

    @Test
    fun profileCarriesEveryOnboardingAnswer() {
        val prefs = FakePreferences().apply {
            gender = "female"
            trainingExperience = "beginner"
            trainingObstacle = "time"
            focusAreas = "plate_photo,smart_overload"
            commitmentDaysPerWeek = 4
            sessionMinutes = 30
            attributionSource = "friend"
            commitmentConfirmedAt = "2026-11-01T10:05:00Z"
            notificationsEnabled = true
            reminderHour = 7
            reminderMinute = 30
        }
        val cloud = buildCloudUserProfile(local, prefs, "ada@example.com", language = "en", timeZone = "Europe/Istanbul")

        assertEquals("ada@example.com", cloud.profile.email)
        assertEquals(
            OnboardingAnswers("female", "beginner", "time", setOf("plate_photo", "smart_overload"), 4, 30),
            cloud.answers,
        )
        assertEquals(CloudUserSettings("friend", "2026-11-01T10:05:00Z", true, 7, 30), cloud.settings)
        assertEquals("en", cloud.language)
        assertEquals("Europe/Istanbul", cloud.timeZone)
        assertFalse(cloud.legacy)
    }

    @Test
    fun keepsLocalEmailWhenPresent() {
        val cloud = buildCloudUserProfile(local.copy(email = "old@example.com"), FakePreferences(), "new@example.com")
        assertEquals("old@example.com", cloud.profile.email)
    }

    @Test
    fun applyRestoresAnswersButNotNotificationPermission() {
        val source = FakePreferences().apply {
            gender = "male"
            trainingExperience = "advanced"
            trainingObstacle = "injury"
            focusAreas = "ai_coach"
            commitmentDaysPerWeek = 5
            sessionMinutes = 60
            notificationsEnabled = true
            reminderHour = 20
        }
        val cloud = buildCloudUserProfile(local, source, null, language = "tr", timeZone = "UTC")
        val target = FakePreferences()
        target.applyCloudProfile(cloud)

        assertEquals("male", target.gender)
        assertEquals("advanced", target.trainingExperience)
        assertEquals("injury", target.trainingObstacle)
        assertEquals("ai_coach", target.focusAreas)
        assertEquals(5, target.commitmentDaysPerWeek)
        assertEquals(60, target.sessionMinutes)
        assertEquals(20, target.reminderHour)
        assertFalse(target.notificationsEnabled)
    }

    @Test
    fun legacyProfileLeavesPreferencesUntouched() {
        val target = FakePreferences().apply { gender = "female" }
        val legacy = buildCloudUserProfile(local, FakePreferences(), null, "tr", "UTC").copy(legacy = true)
        target.applyCloudProfile(legacy)
        assertEquals("female", target.gender)
        assertNull(target.trainingObstacle)
    }

    @Test
    fun planPayloadIncludesAllAnswersAndLocale() {
        val answers = OnboardingAnswers("female", "none", "knowledge", setOf("streak", "ai_coach"), 3, 45)
        val payload = local.toProfilePayload(answers, "en")
        assertEquals("female", payload.gender)
        assertEquals("none", payload.experience)
        assertEquals("knowledge", payload.obstacle)
        assertEquals(listOf("ai_coach", "streak"), payload.focusAreas)
        assertEquals(3, payload.daysPerWeek)
        assertEquals(45, payload.sessionMinutes)
        assertEquals("en", payload.locale)
        assertEquals("dumbbells,pull_up_bar", payload.equipment)
        assertEquals("vegan", payload.dietRestrictions)
    }
}

private class FakePreferences : UserPreferencesRepository {
    override var attributionSource: String? = null
    override var commitmentDaysPerWeek: Int? = null
    override var commitmentConfirmedAt: String? = null
    override var reminderHour: Int = 18
    override var reminderMinute: Int = 0
    override var notificationsEnabled: Boolean = false
    override var streakFreezeUsedWeek: String? = null
    override var gender: String? = null
    override var trainingExperience: String? = null
    override var trainingObstacle: String? = null
    override var focusAreas: String? = null
    override var sessionMinutes: Int? = null
    override var starterChallengeDismissed: Boolean = false
    override var starterGiftClaimed: Boolean = false
}
