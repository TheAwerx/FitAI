package com.fitai.app.auth

import com.fitai.app.sync.CloudUserProfile
import com.fitai.app.sync.FirestoreSyncRemoteDataSource
import com.fitai.app.sync.hasProfile
import com.fitai.app.sync.toCloudProfile
import com.fitai.app.sync.toDocument
import com.fitai.app.util.nowIsoTimestamp

/** `users/{uid}` with a read fallback to the legacy `user_profiles/{uid}` (see `firebase/SCHEMA.md`). */
internal class FirestoreProfileSyncRepository(
    private val remote: FirestoreSyncRemoteDataSource,
) : ProfileSyncRepository {

    override val isFirebaseAvailable: Boolean get() = remote.isAvailable

    override suspend fun pushProfile(uid: String, profile: CloudUserProfile): Result<Unit> = runCatching {
        check(remote.isAvailable) { "Firebase is not configured" }
        remote.setUser(uid, profile.toDocument(updatedAt = nowIsoTimestamp()))
    }

    override suspend fun pullProfile(uid: String): Result<CloudUserProfile?> = runCatching {
        check(remote.isAvailable) { "Firebase is not configured" }
        remote.getUser(uid)?.takeIf { it.hasProfile }?.toCloudProfile()
            ?: remote.getLegacyProfile(uid)?.toCloudProfile()
    }
}
