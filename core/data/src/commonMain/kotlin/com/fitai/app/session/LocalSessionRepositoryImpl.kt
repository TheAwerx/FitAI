package com.fitai.app.session

import com.fitai.app.db.FitAiDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalSessionRepositoryImpl(
    private val database: FitAiDatabase,
    private val settings: Settings,
) : LocalSessionRepository {

    override suspend fun clear() = withContext(Dispatchers.Default) {
        database.transaction {
            database.sessionQueries.deleteAllSessionSets()
            database.sessionQueries.deleteAllSessionExercises()
            database.sessionQueries.deleteAllWorkoutSessions()
            database.sessionQueries.deleteAllExercises()
            database.sessionQueries.deleteAllWorkoutPlans()
            database.sessionQueries.deleteAllMealLogs()
            database.sessionQueries.deleteAllDietPlans()
            database.sessionQueries.deleteAllMeasurements()
            database.sessionQueries.deleteAllGuestTrials()
            database.sessionQueries.deleteAllUsers()
        }
        val language = settings.getStringOrNull(SessionPreferenceKeys.LANGUAGE)
        settings.clear()
        if (language != null) settings[SessionPreferenceKeys.LANGUAGE] = language
        settings[SessionPreferenceKeys.REQUIRES_LOGIN] = true
    }
}
