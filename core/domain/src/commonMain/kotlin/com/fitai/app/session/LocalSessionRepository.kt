package com.fitai.app.session

/** Keys shared by navigation prefs and the sign-out wipe. */
object SessionPreferenceKeys {
    const val REQUIRES_LOGIN = "requires_login"
    const val LANGUAGE = "language_override"
    /** Dev-flavor trial ledger (`LocalTrialRepository`); kept on sign-out like the server would. */
    const val DEV_TRIAL_LEDGER = "dev_trial_ledger"
}

/** Wipes the on-device account: database, cached prefs, and the flag that opens login. Language stays. */
interface LocalSessionRepository {
    suspend fun clear()
}
