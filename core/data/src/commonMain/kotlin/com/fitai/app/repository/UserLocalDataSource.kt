package com.fitai.app.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.db.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserLocalDataSource(
    private val database: FitAiDatabase,
) {
    fun observeLatestUser(): Flow<Users?> =
        database.usersQueries
            .selectLatestUser()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)

    suspend fun getLatestUser(): Users? = withContext(Dispatchers.Default) {
        database.usersQueries.selectLatestUser().executeAsOneOrNull()
    }

    suspend fun insertUser(
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
    ) = withContext(Dispatchers.Default) {
        database.usersQueries.insertUser(
            id = id,
            name = name,
            email = email,
            age = age,
            height_cm = heightCm,
            weight_kg = weightKg,
            goal = goal,
            activity_level = activityLevel,
            equipment = equipment,
            diet_restrictions = dietRestrictions,
            created_at = createdAt,
            firebase_uid = null,
            updated_at = updatedAt,
        )
    }

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
    ) = withContext(Dispatchers.Default) {
        database.usersQueries.updateUserProfile(
            name = name,
            email = email,
            age = age,
            height_cm = heightCm,
            weight_kg = weightKg,
            goal = goal,
            activity_level = activityLevel,
            equipment = equipment,
            diet_restrictions = dietRestrictions,
            updated_at = updatedAt,
            id = id,
        )
    }

    suspend fun linkFirebaseUid(localUserId: String, firebaseUid: String, updatedAt: String) =
        withContext(Dispatchers.Default) {
            database.usersQueries.updateFirebaseUid(
                firebase_uid = firebaseUid,
                updated_at = updatedAt,
                id = localUserId,
            )
        }
}
