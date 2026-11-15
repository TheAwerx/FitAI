package com.fitai.app.auth

/** Clears the platform account picker session (Google Credential Manager on Android). */
interface PlatformAuthSession {
    suspend fun clear()
}
