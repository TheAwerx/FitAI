package com.fitai.app.auth

import com.fitai.app.model.LocalUserProfile

interface ProfileSyncRepository {
    val isFirebaseAvailable: Boolean
    suspend fun pushLocalProfile(uid: String, local: LocalUserProfile): Result<Unit>
    /** Cloud copy written by [pushLocalProfile]; null when the account has none. */
    suspend fun pullProfile(uid: String): Result<LocalUserProfile?>
}
