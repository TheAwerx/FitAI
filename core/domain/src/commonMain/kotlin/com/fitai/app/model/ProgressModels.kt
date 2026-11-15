package com.fitai.app.model

data class BodyMeasurement(
    val id: String,
    val userId: String,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val armCm: Double? = null,
    val photoUrl: String? = null,
    val measuredAt: String,
)
