package com.fitai.app.repository

import com.fitai.app.model.LocalUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalUserRepositoryImpl(
    private val local: UserLocalDataSource,
) : LocalUserRepository {

    override fun observeLatestUser(): Flow<LocalUserProfile?> =
        local.observeLatestUser().map { it?.toLocalUserProfile() }

    override suspend fun getLatestUser(): LocalUserProfile? =
        local.getLatestUser()?.toLocalUserProfile()

    override suspend fun insertGuestProfile(
        id: String,
        name: String,
        email: String,
        age: Long,
        heightCm: Double,
        weightKg: Double,
        goal: String,
        activityLevel: String,
        equipment: String,
        dietRestrictions: String,
        createdAt: String,
        updatedAt: String,
    ) {
        local.insertUser(
            id = id,
            name = name,
            email = email,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            activityLevel = activityLevel,
            equipment = equipment,
            dietRestrictions = dietRestrictions,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    override suspend fun updateProfile(
        id: String,
        name: String,
        email: String,
        age: Long,
        heightCm: Double,
        weightKg: Double,
        goal: String,
        activityLevel: String,
        equipment: String,
        dietRestrictions: String,
        updatedAt: String,
    ) {
        local.updateProfile(
            id = id,
            name = name,
            email = email,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            activityLevel = activityLevel,
            equipment = equipment,
            dietRestrictions = dietRestrictions,
            updatedAt = updatedAt,
        )
    }

    override suspend fun linkFirebaseUid(localUserId: String, firebaseUid: String, updatedAt: String) {
        local.linkFirebaseUid(localUserId, firebaseUid, updatedAt)
    }
}
