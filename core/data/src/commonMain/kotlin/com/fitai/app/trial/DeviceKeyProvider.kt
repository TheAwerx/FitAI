package com.fitai.app.trial

enum class DevicePlatform(val code: String) { Android("android"), Ios("ios") }

/** Where [DeviceKey.value] came from; shown in the debug line of Settings. */
enum class DeviceKeySource(val code: String) {
    /** `Settings.Secure.ANDROID_ID`: same for every reinstall signed with the same key (Android 8+). */
    AndroidId("android_id"),
    /** UUID in the iOS Keychain (this device only, not synced): survives reinstall. */
    Keychain("keychain"),
    /** `identifierForVendor`: survives reinstall only while another app of the vendor stays installed. */
    VendorId("vendor_id"),
    /** Random id in app storage: lost on reinstall. Last resort. */
    Generated("generated"),
}

/**
 * [value] is a SHA-256 hex of the platform id, so the raw id never leaves the device. The server
 * HMACs it again with its own secret before storing (`device_trials/{hash}`).
 */
data class DeviceKey(
    val value: String,
    val platform: DevicePlatform,
    val source: DeviceKeySource,
)

/**
 * Stable per-device key for the one-trial-per-device rule. Platform implementations:
 * `AndroidDeviceKeyProvider` (androidMain) and `IosDeviceKeyProvider` (iosMain), registered in
 * composeApp's `platformDriverModule()`.
 */
interface DeviceKeyProvider {
    suspend fun deviceKey(): DeviceKey

    /** Debug builds: Settings shows the trial state and a short prefix of the key. */
    val showsDiagnostics: Boolean
}

internal const val DEVICE_KEY_SALT = "fitai-device:v1:"

internal fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
