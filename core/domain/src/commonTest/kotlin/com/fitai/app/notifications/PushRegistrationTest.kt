package com.fitai.app.notifications

import com.fitai.app.domain.UseCaseState
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushRegistrationTest {
    private val prefs = Prefs()
    private val scheduler = Scheduler()
    private val tokens = Tokens()
    private val devices = Devices()
    private val sync = SyncPushRegistrationUseCase(prefs, scheduler, tokens, devices)

    @AfterTest
    fun resetLanguage() {
        AppLanguage.override = null
    }

    @Test
    fun registersTheTokenWithLanguageAndPlatform() = runTest {
        AppLanguage.override = "en"
        prefs.notificationsEnabled = true
        tokens.token = "fcm-1"
        assertEquals(UseCaseState.Success(PushRegistrationStatus.Registered), sync())
        val device = devices.registered!!
        assertEquals("fcm-1", device.token)
        assertEquals("ios", device.platform)
        assertEquals("2.0", device.appVersion)
        assertEquals("en", device.language)
        assertTrue(device.timeZone.isNotBlank())
    }

    @Test
    fun removesTheDeviceWhenRemindersAreOffOrNotPermitted() = runTest {
        prefs.notificationsEnabled = false
        tokens.token = "fcm-1"
        assertEquals(UseCaseState.Success(PushRegistrationStatus.Disabled), sync())
        assertEquals(1, devices.unregistered)
        prefs.notificationsEnabled = true
        scheduler.granted = false
        assertEquals(UseCaseState.Success(PushRegistrationStatus.Disabled), sync())
        assertEquals(2, devices.unregistered)
        assertNull(devices.registered)
    }

    @Test
    fun keepsAnEarlierRegistrationWhileNoTokenIsAvailable() = runTest {
        prefs.notificationsEnabled = true
        tokens.token = null
        assertEquals(UseCaseState.Success(PushRegistrationStatus.Unavailable), sync())
        devices.active = true
        assertEquals(UseCaseState.Success(PushRegistrationStatus.Registered), sync())
        assertEquals(0, devices.unregistered)
    }

    @Test
    fun signOutDropsTheDeviceAndTheToken() = runTest {
        UnregisterPushDeviceUseCase(devices, tokens)()
        assertEquals(1, devices.unregistered)
        assertTrue(tokens.invalidated)
    }

    @Test
    fun typeSwitchesUpdateOneTypeOnly() {
        val result = SetPushNotificationTypeUseCase(prefs)(PushNotificationType.MealLogging, false)
        assertEquals(UseCaseState.Success(PushNotificationTypes(mealLogging = false)), result)
        assertFalse(prefs.pushNotificationTypes.mealLogging)
        assertTrue(prefs.pushNotificationTypes.isEnabled(PushNotificationType.WorkoutLate))
    }

    private class Scheduler : ReminderScheduler {
        var granted = true
        override suspend fun schedule(schedule: ReminderSchedule) = Unit
        override suspend fun cancelAll() = Unit
        override suspend fun isPermissionGranted(): Boolean = granted
    }

    private class Tokens : PushTokenProvider {
        var token: String? = null
        var invalidated = false
        override val platform = "ios"
        override val appVersion = "2.0"
        override suspend fun currentToken(): String? = token
        override val tokenRefreshes: Flow<String> = emptyFlow()
        override suspend fun invalidateToken() {
            invalidated = true
        }
    }

    private class Devices : PushDeviceRepository {
        var registered: PushDevice? = null
        var unregistered = 0
        var active = false
        override suspend fun register(device: PushDevice): PushRegistrationStatus {
            registered = device
            return PushRegistrationStatus.Registered
        }

        override suspend fun unregister() {
            unregistered++
        }

        override suspend fun isRegisteredForCurrentAccount(): Boolean = active
    }

    private class Prefs : UserPreferencesRepository {
        override var attributionSource: String? = null
        override var commitmentDaysPerWeek: Int? = null
        override var commitmentConfirmedAt: String? = null
        override var reminderHour: Int = 18
        override var reminderMinute: Int = 0
        override var notificationsEnabled: Boolean = false
        override var pushNotificationTypes: PushNotificationTypes = PushNotificationTypes()
        override var streakFreezeUsedWeek: String? = null
        override var gender: String? = null
        override var trainingExperience: String? = null
        override var trainingObstacle: String? = null
        override var focusAreas: String? = null
        override var sessionMinutes: Int? = null
        override var starterChallengeDismissed: Boolean = false
        override var starterGiftClaimed: Boolean = false
    }
}
