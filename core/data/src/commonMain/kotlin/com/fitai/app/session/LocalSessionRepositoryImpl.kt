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
            database.sessionQueries.deleteAllCustomFoods()
            database.sessionQueries.deleteAllDietPlans()
            database.sessionQueries.deleteAllMeasurements()
            database.sessionQueries.deleteAllGuestTrials()
            database.sessionQueries.deleteAllUsers()
        }
        val language = settings.getStringOrNull(SessionPreferenceKeys.LANGUAGE)
        val devTrialLedger = settings.getStringOrNull(SessionPreferenceKeys.DEV_TRIAL_LEDGER)
        settings.clear()
        if (language != null) settings[SessionPreferenceKeys.LANGUAGE] = language
        if (devTrialLedger != null) settings[SessionPreferenceKeys.DEV_TRIAL_LEDGER] = devTrialLedger
        settings[SessionPreferenceKeys.REQUIRES_LOGIN] = true
    }
}
