package com.fitai.app.notifications

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.util.appLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/*
 * Server-sent "smart" notifications (firebase/functions/src/notifications). The app registers this
 * installation's FCM token under `users/{uid}/devices/{installationId}`; Cloud Functions decide when a
 * nudge is due. How they split the work with the local reminders: firebase/SCHEMA.md, "Push notifications".
 */

/** Dynamic nudge types the user can switch off in Settings; stored in `users/{uid}.notificationTypes`. */
data class PushNotificationTypes(
    /** Training day, reminder time + 90 min passed, no workout yet. */
    val workoutLate: Boolean = true,
    /** Nothing logged around a mealtime. Also gates the local 12:30 meal reminder. */
    val mealLogging: Boolean = true,
    /** Evening heads-up when today would end the streak. */
    val streakRisk: Boolean = true,
    /** At most two nudges after a few days off. */
    val comeback: Boolean = true,
)

enum class PushNotificationType { WorkoutLate, MealLogging, StreakRisk, Comeback }

fun PushNotificationTypes.isEnabled(type: PushNotificationType): Boolean = when (type) {
    PushNotificationType.WorkoutLate -> workoutLate
    PushNotificationType.MealLogging -> mealLogging
    PushNotificationType.StreakRisk -> streakRisk
    PushNotificationType.Comeback -> comeback
}

fun PushNotificationTypes.with(type: PushNotificationType, enabled: Boolean): PushNotificationTypes = when (type) {
    PushNotificationType.WorkoutLate -> copy(workoutLate = enabled)
    PushNotificationType.MealLogging -> copy(mealLogging = enabled)
    PushNotificationType.StreakRisk -> copy(streakRisk = enabled)
    PushNotificationType.Comeback -> copy(comeback = enabled)
}

/** Platform push token source (FCM on Android, FirebaseMessaging via Swift on iOS). */
interface PushTokenProvider {
    /** `android` or `ios`, as in the device document. */
    val platform: String
    val appVersion: String

    /** The current FCM token, or null when push is unavailable right now (no Firebase, no APNs token yet). */
    suspend fun currentToken(): String?

    /** Tokens FCM issues while the app is running (rotation, restore). */
    val tokenRefreshes: Flow<String>

    /**
     * Drops this installation's FCM token (sign-out). Pushes still addressed to it fail as "not
     * registered", so the server deletes the device document even if the delete below never got out.
     */
    suspend fun invalidateToken()
}

data class PushDevice(
    val token: String,
    val platform: String,
    val appVersion: String,
    val language: String,
    val timeZone: String,
)

enum class PushRegistrationStatus {
    /** The device document for the signed-in account holds this installation's token. */
    Registered,
    /** No real (non-anonymous) sign-in, so there is no account to send to. */
    NotSignedIn,
    /** No token (Firebase not configured, APNs not ready, offline first launch). */
    Unavailable,
    /** Notifications are off or not permitted on this device; the device document was removed. */
    Disabled,
}

interface PushDeviceRepository {
    /**
     * Writes `users/{uid}/devices/{installationId}` for the signed-in account. An identical write from
     * the last few days is skipped.
     */
    suspend fun register(device: PushDevice): PushRegistrationStatus

    /** Deletes this installation's device document, if the signed-in account has one. Never throws. */
    suspend fun unregister()

    /** True when this installation's token is stored for the account that is signed in now. */
    suspend fun isRegisteredForCurrentAccount(): Boolean
}

/**
 * Brings the device document in line with the reminder switch and OS permission: registers (or
 * refreshes) the token when notifications are on, removes it when they are off.
 */
class SyncPushRegistrationUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val pushTokenProvider: PushTokenProvider,
    private val pushDeviceRepository: PushDeviceRepository,
) {
    suspend operator fun invoke(): UseCaseState<PushRegistrationStatus> = useCaseCatching {
        if (!userPreferencesRepository.notificationsEnabled || !reminderScheduler.isPermissionGranted()) {
            pushDeviceRepository.unregister()
            return@useCaseCatching PushRegistrationStatus.Disabled
        }
        val token = pushTokenProvider.currentToken()
        if (token.isNullOrBlank()) {
            // Keep an earlier registration: the token is unchanged until FCM says otherwise.
            return@useCaseCatching if (pushDeviceRepository.isRegisteredForCurrentAccount()) {
                PushRegistrationStatus.Registered
            } else {
                PushRegistrationStatus.Unavailable
            }
        }
        pushDeviceRepository.register(
            PushDevice(
                token = token,
                platform = pushTokenProvider.platform,
                appVersion = pushTokenProvider.appVersion,
                language = appLocale(),
                timeZone = TimeZone.currentSystemDefault().id,
            ),
        )
    }
}

/** Before sign-out: the account must stop sending to this device. */
class UnregisterPushDeviceUseCase(
    private val pushDeviceRepository: PushDeviceRepository,
    private val pushTokenProvider: PushTokenProvider,
) {
    suspend operator fun invoke(): UseCaseState<Unit> = useCaseCatching {
        pushDeviceRepository.unregister()
        pushTokenProvider.invalidateToken()
    }
}

class ObservePushTokenRefreshesUseCase(
    private val pushTokenProvider: PushTokenProvider,
) {
    operator fun invoke(): Flow<String> = pushTokenProvider.tokenRefreshes
}

class IsServerPushActiveUseCase(
    private val pushDeviceRepository: PushDeviceRepository,
) {
    suspend operator fun invoke(): UseCaseState<Boolean> = useCaseCatching { pushDeviceRepository.isRegisteredForCurrentAccount() }
}

class GetPushNotificationTypesUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): UseCaseState<PushNotificationTypes> = useCaseCatching { userPreferencesRepository.pushNotificationTypes }
}

/** Stores the switch; it reaches `users/{uid}` through the profile sync. Rebuild local reminders after it. */
class SetPushNotificationTypeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(type: PushNotificationType, enabled: Boolean): UseCaseState<PushNotificationTypes> = useCaseCatching {
        val updated = userPreferencesRepository.pushNotificationTypes.with(type, enabled)
        userPreferencesRepository.pushNotificationTypes = updated
        updated
    }
}
