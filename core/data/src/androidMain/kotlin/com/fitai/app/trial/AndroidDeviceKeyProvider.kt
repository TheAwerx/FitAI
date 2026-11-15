package com.fitai.app.trial

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * `ANDROID_ID` is scoped to the app signing key, user and device since Android 8: reinstalling the
 * same app gives the same value; a factory reset (or a rooted device rewriting it) gives a new one.
 * Play Integrity would be the stronger follow-up (docs/trial.md).
 */
class AndroidDeviceKeyProvider(
    context: Context,
) : DeviceKeyProvider {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cached: DeviceKey? = null

    override val showsDiagnostics: Boolean =
        (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override suspend fun deviceKey(): DeviceKey = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { compute() }.also { cached = it }
    }

    @SuppressLint("HardwareIds")
    private fun compute(): DeviceKey {
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it !in BROKEN_ANDROID_IDS }
        if (androidId != null) {
            return DeviceKey(sha256Hex("${DEVICE_KEY_SALT}android:$androidId"), DevicePlatform.Android, DeviceKeySource.AndroidId)
        }
        val prefs = appContext.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
        val generated = prefs.getString(KEY_GENERATED, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_GENERATED, it).apply() }
        return DeviceKey(sha256Hex("${DEVICE_KEY_SALT}android-generated:$generated"), DevicePlatform.Android, DeviceKeySource.Generated)
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).toHex()

    private companion object {
        /** Separate from the app settings, so sign-out doesn't clear it. */
        const val FALLBACK_PREFS = "fitai_device"
        const val KEY_GENERATED = "generated_device_id"
        /** Old emulator / Android 2.2 builds returned this for every device. */
        val BROKEN_ANDROID_IDS = setOf("9774d56d682e549c", "0000000000000000")
    }
}
