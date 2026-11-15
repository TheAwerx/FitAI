package com.fitai.app.ui.platform

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.fitai.app.auth.PlatformAuthSession

class AndroidPlatformAuthSession(
    context: Context,
) : PlatformAuthSession {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    override suspend fun clear() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
