package com.fitai.app.notifications

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.sync.DeviceDocument
import com.fitai.app.sync.FirestorePaths
import com.fitai.app.sync.toDocument
import com.fitai.app.util.epochMillis
import com.fitai.app.util.nowIsoTimestamp
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.firestore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/** What this installation last wrote, so launches don't rewrite an unchanged device document. */
internal class PushRegistrationStore(private val settings: Settings) {

    /** Random per-install id; the device document id. Cleared with the other settings on sign-out. */
    val installationId: String
        get() = settings.getStringOrNull(KEY_INSTALLATION_ID)
            ?: (1..32).joinToString("") { Random.nextInt(0, 16).toString(16) }.also { settings[KEY_INSTALLATION_ID] = it }

    var registeredUid: String?
        get() = settings.getStringOrNull(KEY_UID)
        set(value) = if (value == null) settings.remove(KEY_UID) else settings[KEY_UID] = value

    var fingerprint: String?
        get() = settings.getStringOrNull(KEY_FINGERPRINT)
        set(value) = if (value == null) settings.remove(KEY_FINGERPRINT) else settings[KEY_FINGERPRINT] = value

    var writtenAtMillis: Long
        get() = settings.getLong(KEY_WRITTEN_AT, 0L)
        set(value) = settings.set(KEY_WRITTEN_AT, value)

    fun clear() {
        settings.remove(KEY_UID)
        settings.remove(KEY_FINGERPRINT)
        settings.remove(KEY_WRITTEN_AT)
    }

    private companion object {
        const val KEY_INSTALLATION_ID = "push_installation_id"
        const val KEY_UID = "push_registered_uid"
        const val KEY_FINGERPRINT = "push_registered_fingerprint"
        const val KEY_WRITTEN_AT = "push_registered_at_ms"
    }
}

/**
 * `users/{uid}/devices/{installationId}` (firebase/SCHEMA.md). Only the signed-in, non-anonymous owner can
 * write it. Writes are awaited briefly: offline, the SDK keeps the write queued and it goes out later.
 */
class FirestorePushDeviceRepository(settings: Settings) : PushDeviceRepository {
    private val store = PushRegistrationStore(settings)
    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    private fun accountUid(): String? {
        if (!initialized) return null
        val user = Firebase.auth.currentUser ?: return null
        return if (user.isAnonymous) null else user.uid
    }

    private fun deviceRef(uid: String): DocumentReference =
        Firebase.firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.DEVICES).document(store.installationId)

    override suspend fun register(device: PushDevice): PushRegistrationStatus {
        if (!initialized) return PushRegistrationStatus.Unavailable
        val uid = accountUid() ?: return PushRegistrationStatus.NotSignedIn
        // Another account's document can't be touched any more (owner-only rules); start fresh.
        if (store.registeredUid != null && store.registeredUid != uid) store.clear()
        val fingerprint = listOf(uid, device.token, device.platform, device.language, device.timeZone, device.appVersion)
            .joinToString("|")
        val now = epochMillis()
        if (store.fingerprint == fingerprint && now - store.writtenAtMillis < REFRESH_MILLIS) {
            return PushRegistrationStatus.Registered
        }
        val doc = device.toDocument(nowIsoTimestamp())
        val confirmed = withTimeoutOrNull(WRITE_TIMEOUT) {
            deviceRef(uid).set(DeviceDocument.serializer(), doc) { encodeDefaults = true }
            true
        } == true
        store.registeredUid = uid
        if (confirmed) {
            store.fingerprint = fingerprint
            store.writtenAtMillis = now
        } else {
            // Still queued by the SDK; write again next time instead of trusting it.
            store.fingerprint = null
        }
        return PushRegistrationStatus.Registered
    }

    override suspend fun unregister() {
        val registered = store.registeredUid ?: return
        try {
            if (accountUid() == registered) {
                withTimeoutOrNull(WRITE_TIMEOUT) { deviceRef(registered).delete() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Push device delete failed", e)
        }
        store.clear()
    }

    override suspend fun isRegisteredForCurrentAccount(): Boolean {
        val registered = store.registeredUid ?: return false
        return registered == accountUid()
    }

    private companion object {
        val WRITE_TIMEOUT = 5.seconds
        /** Rewrite at least weekly so `updatedAt` shows the installation is alive. */
        const val REFRESH_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
