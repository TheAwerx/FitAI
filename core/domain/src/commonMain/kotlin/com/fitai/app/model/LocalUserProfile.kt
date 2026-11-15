package com.fitai.app.model

data class LocalUserProfile(
    val id: String,
    val name: String,
    val email: String,
    val age: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val equipment: String,
    val dietRestrictions: String,
    val firebaseUid: String?,
    val createdAt: String,
    val updatedAt: String,
)

fun LocalUserProfile.toProfilePayload() = UserProfilePayload(
    name = name,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    goal = goal,
    activityLevel = activityLevel,
    equipment = equipment,
    dietRestrictions = dietRestrictions,
)
