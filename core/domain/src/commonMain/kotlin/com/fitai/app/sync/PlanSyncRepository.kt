package com.fitai.app.sync

interface PlanSyncRepository {
    val isFirebaseAvailable: Boolean
    suspend fun pushPlansToFirestore(uid: String, userId: String): Result<Unit>
}
