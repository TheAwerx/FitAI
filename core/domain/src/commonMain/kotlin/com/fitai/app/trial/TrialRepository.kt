package com.fitai.app.trial

import com.fitai.app.model.TrialStatus

interface TrialRepository {
    val isFirebaseAvailable: Boolean
    suspend fun ensureAnonymousAuth(): Result<String>
    suspend fun initGuestTrial(): Result<TrialStatus>
    suspend fun getTrialStatus(): Result<TrialStatus>
    suspend fun canAccessAiFeatures(): Result<Boolean>
}
