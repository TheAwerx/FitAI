package com.fitai.app.sync

import com.fitai.app.util.nowIsoTimestamp
import kotlinx.datetime.TimeZone

internal class FirestoreUserDataSyncRepository(
    private val local: CloudSyncLocalDataSource,
    private val remote: FirestoreSyncRemoteDataSource,
) : UserDataSyncRepository {

    override val isFirebaseAvailable: Boolean get() = remote.isAvailable

    override suspend fun pushAll(uid: String, localUserId: String): Result<Unit> = runCatching {
        check(remote.isAvailable) { "Firebase is not configured" }
        val now = nowIsoTimestamp()
        val zone = TimeZone.currentSystemDefault()
        val snapshot = CloudDataSnapshot(
            workoutPlans = local.workoutPlans(localUserId).map { (plan, exercises) -> plan.toDocument(exercises, now) },
            dietPlans = local.dietPlans(localUserId).map { it.toDocument(now) },
            sessions = local.sessions(localUserId).map { it.toDocument(now, zone) },
            meals = local.meals(localUserId).map { it.toDocument(now, zone) },
            measurements = local.measurements(localUserId).map { it.toDocument(now, zone) },
            customFoods = local.customFoods(localUserId).map { it.toDocument(now) },
        )
        remote.writeAll(uid, snapshot)
    }

    override suspend fun restoreAll(uid: String, localUserId: String, keepLocalPlans: Boolean): Result<CloudRestoreSummary> =
        runCatching {
            check(remote.isAvailable) { "Firebase is not configured" }
            local.restore(localUserId, remote.readAll(uid), keepLocalPlans)
        }
}

class NoOpUserDataSyncRepository : UserDataSyncRepository {
    override val isFirebaseAvailable: Boolean = false
    override suspend fun pushAll(uid: String, localUserId: String): Result<Unit> = Result.success(Unit)
    override suspend fun restoreAll(uid: String, localUserId: String, keepLocalPlans: Boolean): Result<CloudRestoreSummary> =
        Result.success(CloudRestoreSummary())
}
