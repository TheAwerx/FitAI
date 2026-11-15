package com.fitai.app.trial

import com.fitai.app.model.TrialState
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Request body of `getTrialStatus` and `startTrial`. */
@Serializable
internal data class TrialDeviceRequest(
    val deviceKey: String,
    val platform: String,
)

/** Response of both trial callables (firebase/functions/src/trial.ts `TrialStatusResponse`); extra fields are ignored. */
@Serializable
internal data class TrialStatusDto(
    val state: String = TrialState.Eligible.code,
    val started: Boolean = false,
    val trialDays: Int = TrialPolicy.TRIAL_DAYS,
    val startedAt: String? = null,
    val endsAt: String? = null,
    val serverTime: String? = null,
    val daysRemaining: Int = 0,
)

internal fun TrialStatusDto.toSnapshot(accountId: String?, deviceNowMillis: Long): TrialSnapshot = TrialSnapshot(
    accountId = accountId,
    state = TrialState.fromCode(state) ?: TrialState.Eligible,
    startedAtMillis = TrialPolicy.parseIsoMillis(startedAt),
    endsAtMillis = TrialPolicy.parseIsoMillis(endsAt),
    serverTimeMillis = TrialPolicy.parseIsoMillis(serverTime) ?: deviceNowMillis,
    fetchedAtMillis = deviceNowMillis,
)

/**
 * Last server answer in app settings (cleared on sign-out with the other settings), plus the
 * "trial requested before sign-up" flag.
 */
class TrialStatusCache(
    private val settings: Settings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): TrialSnapshot? = settings.getStringOrNull(KEY_SNAPSHOT)
        ?.let { raw -> runCatching { json.decodeFromString(StoredSnapshot.serializer(), raw) }.getOrNull() }
        ?.toSnapshot()

    fun write(snapshot: TrialSnapshot) {
        settings[KEY_SNAPSHOT] = json.encodeToString(StoredSnapshot.serializer(), StoredSnapshot.of(snapshot))
    }

    var trialRequested: Boolean
        get() = settings.getBoolean(KEY_REQUESTED, false)
        set(value) = if (value) settings.set(KEY_REQUESTED, true) else settings.remove(KEY_REQUESTED)

    @Serializable
    private data class StoredSnapshot(
        val accountId: String? = null,
        val state: String,
        val startedAtMillis: Long? = null,
        val endsAtMillis: Long? = null,
        val serverTimeMillis: Long,
        val fetchedAtMillis: Long,
    ) {
        fun toSnapshot(): TrialSnapshot? = TrialState.fromCode(state)?.let {
            TrialSnapshot(accountId, it, startedAtMillis, endsAtMillis, serverTimeMillis, fetchedAtMillis)
        }

        companion object {
            fun of(s: TrialSnapshot) = StoredSnapshot(
                s.accountId, s.state.code, s.startedAtMillis, s.endsAtMillis, s.serverTimeMillis, s.fetchedAtMillis,
            )
        }
    }

    private companion object {
        const val KEY_SNAPSHOT = "trial_status_snapshot"
        const val KEY_REQUESTED = "trial_requested"
    }
}
