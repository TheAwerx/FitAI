package com.fitai.app.coach

import com.fitai.app.model.CoachMessage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class CoachHistoryRepositoryImpl(
    private val settings: Settings,
    private val json: Json = CoachJson,
) : CoachHistoryRepository {

    private val serializer = ListSerializer(CoachMessage.serializer())

    override suspend fun loadHistory(userId: String): List<CoachMessage> {
        val raw = settings.getStringOrNull(key(userId)) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrElse {
            settings.remove(key(userId))
            emptyList()
        }
    }

    override suspend fun saveHistory(userId: String, messages: List<CoachMessage>) {
        settings[key(userId)] = json.encodeToString(serializer, messages.takeLast(CoachLimits.MAX_HISTORY_STORED))
    }

    override suspend fun clearHistory(userId: String) {
        settings.remove(key(userId))
    }

    private fun key(userId: String) = "$KEY_PREFIX$userId"

    private companion object {
        const val KEY_PREFIX = "coach_history_"
    }
}

internal val CoachJson = Json { ignoreUnknownKeys = true }
