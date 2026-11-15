package com.fitai.app.repository

import com.fitai.app.db.Users
import com.fitai.app.model.LocalUserProfile

internal fun Users.toLocalUserProfile() = LocalUserProfile(
    id = id,
    name = name,
    email = email,
    age = age,
    heightCm = height_cm,
    weightKg = weight_kg,
    goal = goal,
    activityLevel = activity_level,
    equipment = equipment,
    dietRestrictions = diet_restrictions,
    firebaseUid = firebase_uid,
    createdAt = created_at,
    updatedAt = updated_at,
)
