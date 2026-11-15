package com.fitai.app.auth

import com.fitai.app.sync.CloudUserProfile

/** The account document `users/{uid}` (see `firebase/SCHEMA.md`). */
interface ProfileSyncRepository {
    val isFirebaseAvailable: Boolean
    suspend fun pushProfile(uid: String, profile: CloudUserProfile): Result<Unit>

    /**
     * `users/{uid}`, else the legacy `user_profiles/{uid}` ([CloudUserProfile.legacy]); null when the
     * account has neither.
     */
    suspend fun pullProfile(uid: String): Result<CloudUserProfile?>
}
