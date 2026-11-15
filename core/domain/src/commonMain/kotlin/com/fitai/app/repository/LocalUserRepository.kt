package com.fitai.app.repository

import com.fitai.app.model.LocalUserProfile
import kotlinx.coroutines.flow.Flow

interface LocalUserRepository {
    fun observeLatestUser(): Flow<LocalUserProfile?>
    suspend fun getLatestUser(): LocalUserProfile?
    suspend fun insertGuestProfile(
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
    )
    suspend fun updateProfile(
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
    )
    suspend fun linkFirebaseUid(localUserId: String, firebaseUid: String, updatedAt: String)
}
